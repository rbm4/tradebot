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
var binanceApi = new Binance({
    key: process.env.BINANCE_KEY,
    secret: process.env.BINANCE_SECRET,
})
var balancesObj = {}
var balancesObjBinance = {}
var ticker = {}

// Initialization
init();

function init() {
    setTimeout(async () => {
        try {
           // await runBinanceBlock();
            await runMbtcBlock();
            
         }
         catch (e) {
            console.log("supress exception")
            console.log(e)
         } finally {
            init();
         }
    },
        process.env.CRAWLER_INTERVAL
    );
}
//Binance script
async function runBinanceBlock(){
    console.log("Running binance block...")
    const accountInfo = await binanceApi.accountInfo()
    for (b of accountInfo.balances){
        balancesObjBinance[b.asset] = b
    }
    await utils.sleep(1000)
    console.log("Loading spefic currency info from binance...")
    var moedas = ["BCH"]
    var minValueSell = {
        "LTC": "0.001",
        "XRP": "0.1",
        "BCH": "0.002",
        "ETH": "0.001",
    }
    var spreadCut =  process.env.MARKET_CUT
    var percentagePriceCalc = process.env.PERCENTAGE_PRICE_CALC //0,90%
    var balanceCutForSpendFiat = process.env.BALANCE_CUT_SPEND_FIAT
    var balanceCutForSpendCripto = process.env.BALANCE_CUT_SPEND_CRYPTO 
    ticker = await binanceApi.tickers(moedas)
    //need to convert fiat into usdt - done using Binance UI
    
    console.log(ticker)
}
//Mercado bitcoin script
async function runMbtcBlock(){
    console.log("Running Mercado Bitcoin block...")
    const accounts = await mbtcTradeApi.listAccounts()
    var balances = await mbtcTradeApi.listBalance(accounts[0].id)
    for (b of balances){
        balancesObj[b.symbol] = b
    }
    await utils.sleep(1000)
    console.log("Loading spefic currency info...")
    var moedas = ["XRP","BCH"]
    const minBrlOrder = 1
    var minValueSell = {
        "LTC": "0.001",
        "XRP": "0.1",
        "BCH": "0.002",
        "ETH": "0.001",
    }
    const openOrders = await mbtcTradeApi.listPositions(accounts[0].id,moedas)
    // Market cut - Do not operate if Lowest trade differs (positively or negatively) from the current trade price in the last 24h
    var spreadCut = 3
    //Percentage price calc - the amount of % higher or lower to buy or sell assets given their last order (mbtc takes 0.7% cut - equivalent to a minimum of 0.007 in this field)
    var percentagePriceCalc = 0.0095
    //Balance cut spend for fiat - the amount of fiat % the script should spend in each run, for a specific ticker, this value is based on the maximum balance, not the available balance
    //It is desireable to spend less % if you have a bigger balance
    var balanceCutForSpendFiat = 0.25
    var balanceCutForSpendCripto = 0.25
    

    ticker = await mbtcTradeApi.tickers(moedas)
    
    await checkMbtcTickers(moedas, spreadCut, accounts, balanceCutForSpendFiat, percentagePriceCalc, balanceCutForSpendCripto, minValueSell, minBrlOrder, openOrders);
    console.log("Remaining open positions")
    console.log(await mbtcTradeApi.listAllPositions(accounts[0].id))
    console.log(`Finished. Queing again in ${process.env.CRAWLER_INTERVAL / 1000} seconds`)
    
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
        await checkAndCancelOrders(openOrders, pair, accounts, highesbidPrice, highestaskPrice);

        await checkMarketSpread(spreadPercentage, spreadCut, accounts, m, balanceCutForSpendFiat, highesbidPrice, percentagePriceCalc, balanceCutForSpendCripto, minValueSell, minBrlOrder,highestaskPrice);
        
        
        console.log("-------------");
        var balances = await mbtcTradeApi.listBalance(accounts[0].id)
        for (b of balances){
            balancesObj[b.symbol] = b
        }

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
    //If current price to buy or sell is farther than this percentage, then cancel the order and create again (if you dont want this feature to avoid losses, just but 100% here)
    const orderDisparity = 0.025; //2.5% multiplier
    for (let o of ordersFromThisTicker) {
        const orderInfo = await mbtcTradeApi.listOrder(accounts[0].id, o.instrument, o.id);
        const orderAmount = orderInfo.qty;
        const price = orderInfo.limitPrice;
        if (o.side == 'buy') {
            console.log(price * orderDisparity + price);
            console.log(highesbidPrice);
            if ((price * orderDisparity + price) < highesbidPrice) {
                console.log('buy order much lower than market value, should cancel');
                mbtcTradeApi.cancelOrder(accounts[0].id, o.instrument, o.id);
                mysql.createOrder(o.qty,price,o.side,pair,0,"MBTC","Cancel, market price too low for buy",o.id)
            }
        } else {
            console.log(price - (price * orderDisparity));
            console.log(highestaskPrice);
            if (price - (price * orderDisparity) > highestaskPrice) {
                console.log('sell order much higher than market value, should cancel');
                mbtcTradeApi.cancelOrder(accounts[0].id, o.instrument, o.id);
                mysql.createOrder(o.qty,price,o.side,pair,0,"MBTC","Cancel, market price too low for sell",o.id)
            }
        }
    }
}

async function checkMarketSpread(spreadPercentage, spreadCut, accounts, m, 
    balanceCutForSpendFiat, highesbidPrice, percentagePriceCalc, balanceCutForSpendCripto, minValueSell, minBrlOrder, highestaskPrice) {
    if (spreadPercentage <= spreadCut && spreadPercentage >= (spreadCut * -1)) {
        console.log(`Market spread less than ${spreadCut}, and higher than ${(spreadCut * -1)} check market prices and currency allocation`);
        //allocate 25% of current balance to operate
        await utils.sleep(1000);
        await mbtcTradeApi.listOrders(accounts[0].id, m);

        await checkBuy(balanceCutForSpendFiat, percentagePriceCalc, m, highestaskPrice,minValueSell,minBrlOrder,accounts);

        await checkSell(highesbidPrice, percentagePriceCalc, m, balanceCutForSpendCripto, minValueSell, minBrlOrder,accounts);
        utils.sleep(1000);
    } else {
        console.log(`Market spread is higher than 3%, dont buy or sell`);
    }
}
var resultBuyQuery
async function checkSell(highesbidPrice, percentagePriceCalc, m, balanceCutForSpendCripto, minValueSell, minBrlOrder,accounts) {
    const sellPrice = highesbidPrice + (highesbidPrice * percentagePriceCalc);
    const criptoBalance = balancesObj[m];
    const availableCriptoCurrentToSpend = (criptoBalance.total * balanceCutForSpendCripto);
    console.log(availableCriptoCurrentToSpend)
    console.log(sellPrice)
    console.log(minValueSell[m])
    console.log((availableCriptoCurrentToSpend * sellPrice))
    console.log(minBrlOrder)
    if (availableCriptoCurrentToSpend > minValueSell[m] &&
        ((availableCriptoCurrentToSpend * sellPrice) > minBrlOrder)) {
        console.log(`Current ${m} allocation balance to sell is higher than available balance, create sell order"`);
        var latestBuyOrderInThisTicker = []
        await mysql.lastOrder(m,"MBTC","buy",function(err, content) {
            if (err) {
            console.log(err);
            // Do something with your error...
            } else {
                resultBuyQuery = content
            }
        });
        await utils.sleep(100)
        latestBuyOrderInThisTicker = resultBuyQuery
        const availableCriptoCurrencyToSpend = (criptoBalance.total * balanceCutForSpendCripto);
        if ((availableCriptoCurrencyToSpend < criptoBalance.available) 
        && (availableCriptoCurrencyToSpend > minValueSell[m])) {
            if (latestBuyOrderInThisTicker.length < 1){
                //create order
                mysql.createOrder(availableCriptoCurrencyToSpend,sellPrice,"sell",`${m}-BRL`,null,"MBTC","Selling based on market value",null)
                mbtcTradeApi.placeOrder(accounts[0].id,`${m}-BRL`,null,sellPrice,availableCriptoCurrencyToSpend,"sell","limit")
            } else {
                var basedOnResult = []
                await mysql.basedOnOrder(latestBuyOrderInThisTicker[0].id,function(err, content) {
                    if (err) {
                    console.log(err);
                    // Do something with your error...
                    } else {
                        basedOnResult = content
                    }
                });
                console.log(latestBuyOrderInThisTicker[0])
                await utils.sleep(100)
                if (basedOnResult > 0) {
                    mysql.createOrder(availableCriptoCurrencyToSpend,sellPrice,"sell",`${m}-BRL`,null,"MBTC","Selling based on market value",null)
                    mbtcTradeApi.placeOrder(accounts[0].id,`${m}-BRL`,null,sellPrice,availableCriptoCurrencyToSpend,"sell","limit")
                } else {
                    var price = (latestBuyOrderInThisTicker[0].price * percentagePriceCalc) + new Number(latestBuyOrderInThisTicker[0].price)
                    console.log(highesbidPrice)
                    if (price < highesbidPrice){
                        console.log("calculated price is lower than current market, set price to market value")
                        price = highesbidPrice
                    }
                    mysql.createOrder(availableCriptoCurrencyToSpend,price,"sell",`${m}-BRL`,latestBuyOrderInThisTicker[0].id,"MBTC","creating order based on last order",null)
                    mbtcTradeApi.placeOrder(accounts[0].id,`${m}-BRL`,null,price,availableCriptoCurrencyToSpend,"sell","limit")
                }
            }
        }
    } else {
        console.log("Not enough balance to sell");
    }
}
var resultQuery
async function checkBuy(balanceCutForSpendFiat,  percentagePriceCalc, m, highestaskPrice,minValueSell,minBrlOrder,accounts) {
    const balanceObjFiat = balancesObj["BRL"];
    var latestBuyOrderInThisTicker = []
    await mysql.lastOrder(m,"MBTC","sell",function(err, content) {
        if (err) {
        console.log(err);
        // Do something with your error...
        } else {
            resultQuery = content
        }
    });
    await utils.sleep(100)
    latestBuyOrderInThisTicker = resultQuery
    const availableFiatCurrencyToSpend = (balanceObjFiat.total * balanceCutForSpendFiat);
    if ((availableFiatCurrencyToSpend < balanceObjFiat.available) 
    && (availableFiatCurrencyToSpend > minBrlOrder)) {
        if (latestBuyOrderInThisTicker.length < 1){
            const price = highestaskPrice - (highestaskPrice * percentagePriceCalc)
            const amount = availableFiatCurrencyToSpend / price
            if (amount > minValueSell[m]){
                //create order
                mysql.createOrder(amount,price,"buy",`${m}-BRL`,null,"MBTC","lower than highestbid normal buy order",null)
                mbtcTradeApi.placeOrder(accounts[0].id,`${m}-BRL`,null,price,amount,"buy","limit")
            }
        } else {
            var basedOnResult = undefined
            await mysql.basedOnOrder(latestBuyOrderInThisTicker[0].id,function(err, content) {
                if (err) {
                console.log(err);
                // Do something with your error...
                } else {
                    basedOnResult = content
                }
            });
            await utils.sleep(50)
            if (basedOnResult.length > 0 ){
                const price = highestaskPrice - (highestaskPrice * percentagePriceCalc)
                const amount = availableFiatCurrencyToSpend / price
                mysql.createOrder(amount,price,"buy",`${m}-BRL`,null,"MBTC","sell order already used, create based on market value",null)
                mbtcTradeApi.placeOrder(accounts[0].id,`${m}-BRL`,null,price,amount,"buy","limit")
            } else{
                var price = (latestBuyOrderInThisTicker[0].price * percentagePriceCalc) + new Number(latestBuyOrderInThisTicker[0].price)
                const amount2 = availableFiatCurrencyToSpend / price
                if (price > highestaskPrice){
                    //console.log("calculated price is lower than current market, set price to market value")
                    price = highestaskPrice
                }
                mysql.createOrder(amount2,price,"buy",`${m}-BRL`,latestBuyOrderInThisTicker[0].id,"MBTC","creating order based on last order",null)
                mbtcTradeApi.placeOrder(accounts[0].id,`${m}-BRL`,null,price,amount2,"buy","limit")

            }
            utils.sleep(10000)
        }
    } else {
        console.log("Not enough balance to buy");
    }
}

