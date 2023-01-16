require("dotenv-safe").config()
const { MercadoBitcoin, MercadoBitcoinTrade } = require("./mercadoBitcoin");
const { Binance } = require("./binance");
const { Utils } = require("./utils")
const { Mysql } = require("./mysql")

// Needed Global variables
var mysql = new Mysql();
var infoApi = new MercadoBitcoin();
var utils = new Utils();
mysql.connect()
var mbtcTradeApi = new MercadoBitcoinTrade({
    key: process.env.KEY,
    secret: process.env.SECRET,
})
var binanceApi = new Binance()
var balancesObj = {}
var ticker = {}

// Initialization
init();

function init() {
    setTimeout(async () => {
        runMbtcBlock();
    },
        process.env.CRAWLER_INTERVAL
    );
}

//Mercado bitcoin script
async function runMbtcBlock(){
    console.log("Loading balance info...")
    const accounts = await mbtcTradeApi.listAccounts()
    const balances = await mbtcTradeApi.listBalance(accounts[0].id)
    for (b of balances){
        balancesObj[b.symbol] = b
    }
    await utils.sleep(1000)
    console.log("Loading spefic currency info...")
    var moedas = ["LTC","XRP","BCH","ETH"]
    const minBrlOrder = 1
    var minValueSell = {
        "LTC": "0.001",
        "XRP": "0.1",
        "BCH": "0.002",
        "ETH": "0.001",
    }
    const openOrders = await mbtcTradeApi.listPositions(accounts[0].id,moedas)
    var spreadCut = 3
    var percentagePriceCalc = 0.011 //1.1%
    var balanceCutForSpendFiat = 0.25
    var balanceCutForSpendCripto = 0.25
    ticker = await mbtcTradeApi.tickers(moedas)
    
    await checkMbtcTickers(moedas, spreadCut, accounts, balanceCutForSpendFiat, percentagePriceCalc, balanceCutForSpendCripto, minValueSell, minBrlOrder, openOrders);
    console.log("Remaining open positions")
    console.log(await mbtcTradeApi.listAllPositions(accounts[0].id))
    console.log(`Finished. Queing again in ${process.env.CRAWLER_INTERVAL / 1000} seconds`)
    init();
}





async function checkMbtcTickers(moedas, spreadCut, accounts, balanceCutForSpendFiat, percentagePriceCalc, balanceCutForSpendCripto, minValueSell, minBrlOrder, openOrders) {
    for (let m of moedas) {
        let pair = `${m}-BRL`;
        let tickerInfo = utils.getTickerInfo(pair, ticker);
        let high = tickerInfo.high;
        let low = tickerInfo.low;
        let current = tickerInfo.last;
        let buy = tickerInfo.buy;
        let sell = tickerInfo.sell;
        let spreadPercentage = 100 - ((100 * tickerInfo.low) / tickerInfo.last);
        console.log(`Pair: ${pair}`);
        console.log(`High: ${high} Low: ${low} Current: ${current} Buy: ${buy} Sell ${sell}`);
        console.log(`SpreadPercentage: ${spreadPercentage}`);
        const book = await infoApi.orderBook(m);
        const highestask = book.asks[0];
        const highestBid = book.bids[0];
        const highestaskPrice = highestask[0];
        const highesbidPrice = highestBid[0];
        await checkMarketSpread(spreadPercentage, spreadCut, accounts, m, balanceCutForSpendFiat, highesbidPrice, percentagePriceCalc, balanceCutForSpendCripto, minValueSell, minBrlOrder);
        
        await checkAndCancelOrders(openOrders, pair, accounts, highesbidPrice, highestaskPrice);
        console.log("-------------");

    }
}
async function checkAndCancelOrders(openOrders, pair, accounts, highesbidPrice, highestaskPrice) {
    console.log("Verify market price and open orders, if they dont meet conditions, cancel then");
    var ordersFromThisTicker = [];
    for (let o of openOrders) {
        if (o.instrument == pair) {
            ordersFromThisTicker = ordersFromThisTicker.concat(o);
        }
    }
    const orderDisparity = 0.05; //5% multiplier
    for (let o of ordersFromThisTicker) {
        const orderInfo = await mbtcTradeApi.listOrder(accounts[0].id, o.instrument, o.id);
        const orderAmount = orderInfo.qty;
        const price = orderInfo.limitPrice;
        if (o.side == 'buy') {
            console.log(price * orderDisparity + price);
            console.log(highesbidPrice);
            if ((price * orderDisparity + price) < highesbidPrice) {
                console.log('buy order much lower than market value, should cancel');
                await utils.sleep(1000);
                await mbtcTradeApi.cancelOrder(accounts[0].id, o.instrument, o.id);
                mysql.createOrder(o.qty,price,o.side,pair,null,"MBTC","Cancel, market price too low for buy",o.id)
            }
        } else {
            console.log(price - (price * orderDisparity));
            console.log(highestaskPrice);
            if (price - (price * orderDisparity) > highestaskPrice) {
                console.log('sell order much higher than market value, should cancel');
                await mbtcTradeApi.cancelOrder(accounts[0].id, o.instrument, o.id);
                await utils.sleep(1000);
                mysql.createOrder(o.qty,price,o.side,pair,null,"MBTC","Cancel, market price too low for sell",o.id)
            }
        }
    }
}

async function checkMarketSpread(spreadPercentage, spreadCut, accounts, m, 
    balanceCutForSpendFiat, highesbidPrice, percentagePriceCalc, balanceCutForSpendCripto, minValueSell, minBrlOrder) {
    if (spreadPercentage <= spreadCut && spreadPercentage >= (spreadCut * -1)) {
        console.log(`Market spread less than ${spreadCut}, and higher than ${(spreadCut * -1)} check market prices and currency allocation`);
        //allocate 25% of current balance to operate
        await utils.sleep(1000);
        await mbtcTradeApi.listOrders(accounts[0].id, m);

        await checkBuy(balanceCutForSpendFiat, percentagePriceCalc, m, highesbidPrice,minValueSell,minBrlOrder);

        await checkSell(highesbidPrice, percentagePriceCalc, m, balanceCutForSpendCripto, minValueSell, minBrlOrder);
        utils.sleep(1000);
    } else {
        console.log(`Market spread is higher than 3%, dont buy or sell`);
    }
}

async function checkSell(highesbidPrice, percentagePriceCalc, m, balanceCutForSpendCripto, minValueSell, minBrlOrder) {
    const sellPrice = highesbidPrice + (highesbidPrice * percentagePriceCalc);
    const criptoBalance = balancesObj[m];
    const availableCriptoCurrentToSpend = (criptoBalance.total * balanceCutForSpendCripto);
    if ((availableCriptoCurrentToSpend < criptoBalance.available) &&
        availableCriptoCurrentToSpend > minValueSell[m] &&
        ((availableCriptoCurrentToSpend * sellPrice) > minBrlOrder)) {
        console.log(`Current ${m} allocation balance to sell is higher than available balance, create sell order"`);
    } else {
        console.log("Not enough balance to sell");
    }
}
var resultQuery
async function checkBuy(balanceCutForSpendFiat,  percentagePriceCalc, m, highesbidPrice,minValueSell,minBrlOrder) {
    const balanceObjFiat = balancesObj["BRL"];
    var latestBuyOrderInThisTicker = []
    await mysql.lastOrder(m,"MBTC","sell",function(err, content) {
        console.log("last order callback")
        if (err) {
        console.log(err);
        // Do something with your error...
        } else {
            console.log(latestBuyOrderInThisTicker)
            resultQuery = content
        }
    });
    console.log(latestBuyOrderInThisTicker)
    await utils.sleep(2000)
    console.log(resultQuery)
    const availableFiatCurrencyToSpend = (balanceObjFiat.total * balanceCutForSpendFiat);
    if ((availableFiatCurrencyToSpend < balanceObjFiat.available) 
    && (availableFiatCurrencyToSpend > minBrlOrder)) {
        console.log("Current fiat allocation balance to buy is higher than available balance, create buy order");
        if (latestBuyOrderInThisTicker.length < 1){
            console.log("No previous order for this ticker, executing new one based on market value")  
            const price = highesbidPrice - (highesbidPrice * percentagePriceCalc)
            const amount = availableFiatCurrencyToSpend / price
            if (amount > minValueSell[m]){
                //create order
                mysql.createOrder(amount,price,"buy",`${m}-BRL`,null,"MBTC","lower than highestbid normal buy order",null)
            }
        } else {
            console.log("check if previous order have been used in based_on_order field ")
            const isOrderUsed = mysql.basedOnOrder(latestBuyOrderInThisTicker.id)
            if (isOrderUsed != undefined){
                console.log("order already used, create orderm from market value")
            }
            utils.sleep(10000)
        }
    } else {
        console.log("Not enough balance to buy");
    }
}

