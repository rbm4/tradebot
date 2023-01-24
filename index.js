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
var exchangeInfoBinance = {}
var ticker = {}

// Initialization
init();

function init() {
    setTimeout(async () => {
        try {
            //await runBinanceBlock();
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
async function runBinanceBlock() {
    console.log("Running binance block...")
    await updateBinanceBalance();
    await utils.sleep(1000)
    console.log("Loading spefic currency info from binance...")
    var moedas = ["BCH"]
    var marketConfigs = {
        percentagePriceCalc: 0.002,
        balanceCutForSpendFiat: 0.15,
        balancecutForSpendCripto: 0.15,
        orderDisparity: 0.025,
        minValueSellBUSD: 10

    }
    var exchangeInfo = await binanceApi.exchangeInfo(moedas)
    for (let i of exchangeInfo.symbols){
        exchangeInfoBinance[i.symbol] = i
    }
    var tickers = await binanceApi.tickers(moedas)
    //need to convert fiat into usdt - done using Binance UI
    for (let m of moedas) {
        let pair = `${m}BUSD`;
        let info = exchangeInfoBinance[pair]
        let filters = info.filters
        console.log(filters)
        let tickerInfo = utils.getTickerInfoBinance(pair, tickers);
        //let high = tickerInfo.high;
        //let low = tickerInfo.low;
        let bid = tickerInfo.bidPrice; //price of buy orders
        let ask = tickerInfo.askPrice; //price of sell orders
        //let spreadPercentage = 100 - ((100 * tickerInfo.low) / tickerInfo.last); 
        console.log(`Pair: ${pair}`);
        console.log(`Bid: ${bid} Ask ${ask}`);
        //console.log(`SpreadPercentage: ${spreadPercentage}`);
        await checkAndCancelBinanceOrders(pair, bid, ask, marketConfigs.orderDisparity);

        await checkMarketSpreadBinance(marketConfigs, pair, bid, ask, m,info);


        console.log("-------------");


    }

    console.log(ticker)
}

async function updateBinanceBalance() {
    const accountInfo = await binanceApi.accountInfo();
    for (b of accountInfo.balances) {
        balancesObjBinance[b.asset] = b;
    }
}

async function checkMarketSpreadBinance(marketConfigs, pair, bid, ask, currency,exchangeInfo) {
    console.log(exchangeInfo)
    var priceFilter = {}
    var lotSize = {}
    for (let f of exchangeInfo.filters){
        if (f.filterType == 'PRICE_FILTER'){
            priceFilter = f
        } else if (f.filterTYpe == 'LOT_SIZE'){
            lotSize = f
        }
    }
    await updateBinanceBalance()
    await checkBuyBinance(currency, marketConfigs, ask, priceFilter, pair);
    await updateBinanceBalance()
    await checkSellBinance(currency, marketConfigs, bid, priceFilter, pair,lotSize);
}
async function checkSellBinance(currency, marketConfigs, bid, priceFilter, pair,lotSize){
    const sellPrice = bid + (bid * marketConfigs.percentagePriceCalc)
    var currentCriptoBalance = balancesObjBinance[currency]
    var latestBuyOrderInThisTicker = []
    await mysql.lastOrder(m, "BINANCE", "BUY", function (err, content) {
        if (err) {
            console.log(err);
            // Do something with your error...
        } else {
            resultQuery = content
        }
    });
    await utils.sleep(100)
    latestBuyOrderInThisTicker = resultQuery
    if ((currentCriptoBalance > lotSize) && (currentCriptoBalance * sellPrice > marketConfigs.minValueSellBUSD)) {
        var formattedPrice = toFixedNotRound(sellPrice, 1);
        if (formattedPrice == bid) {
            formattedPrice = formattedPrice + priceFilter.tickSize;
            var priceComposition = (formattedPrice + "").split(".");
            formattedPrice = `${priceComposition[0]}.${priceComposition[1].substring(0, 3)}`;
        }
        const amount2 = currentFiatBalance / formattedPrice
        if (price > ask) {
            console.log("calculated price is lower than current market, set price to market value")
            price = ask
        }
        var calc = amount2.split(".");
        calc = `${calc[0]}.${calc[1].substring(0, 3)}`;
        console.log(calc);
        if (latestBuyOrderInThisTicker.length < 1) {
            //create order
            mysql.createOrder(availableCriptoCurrencyToSpend, sellPrice, "sell", `${m}-BRL`, null, "MBTC", "Selling based on market value", null)
            await binanceApi.placeOrder(pair,"SELL","LIMIT",formattedPrice,calc)
        } 
    }
}
async function checkBuyBinance(currency, marketConfigs, ask, priceFilter, pair) {
    var currentFiatBalance = balancesObjBinance["BUSD"]; //Binance dollar
    var latestBuyOrderInThisTicker = []
    await mysql.lastOrder(m, "BINANCE", "SELL", function (err, content) {
        if (err) {
            console.log(err);
            // Do something with your error...
        } else {
            resultQuery = content
        }
    });
    await utils.sleep(100)
    latestBuyOrderInThisTicker = resultQuery
    var freeBalance = new Number(currentFiatBalance.free);
    if (freeBalance > marketConfigs.minValueSellBUSD) {
        console.log("I do have balance to spend, try to buy cripto");
        if (latestBuyOrderInThisTicker.length < 1) {
            normalMarketBuyBinance(freeBalance, marketConfigs, ask, currency, priceFilter, pair);
        } else {
            var basedOnResult = undefined
            await mysql.basedOnOrder(latestBuyOrderInThisTicker[0].id, function (err, content) {
                if (err) {
                    console.log(err);
                    // Do something with your error...
                } else {
                    basedOnResult = content
                }
            });
            await utils.sleep(50)
            if (basedOnResult.length > 0) {
                normalMarketBuyBinance(freeBalance, marketConfigs, ask, currency, priceFilter, pair);
            } else {
                var price = new Number(latestBuyOrderInThisTicker[0].price) - (latestBuyOrderInThisTicker[0].price * percentagePriceCalc)
                var formattedPrice = toFixedNotRound(price, 1);
                console.log(formattedPrice);
                if (formattedPrice == ask) {
                    formattedPrice = formattedPrice - priceFilter.tickSize;
                    var priceComposition = (formattedPrice + "").split(".");
                    formattedPrice = `${priceComposition[0]}.${priceComposition[1].substring(0, 3)}`;
                }
                const amount2 = currentFiatBalance / formattedPrice
                if (price > ask) {
                    console.log("calculated price is lower than current market, set price to market value")
                    price = ask
                }
                var calc = amount2.split(".");
                calc = `${calc[0]}.${calc[1].substring(0, 3)}`;
                console.log(calc);

                mysql.createOrder(calc, formattedPrice, "BUY", pair, latestBuyOrderInThisTicker[0].id, "BINANCE", "creating order based on last order", null)
                //await binanceApi.placeOrder(pair,"BUY","LIMIT",formattedPrice,calc)

            }
            utils.sleep(10000)
        }

        
    }
}

function normalMarketBuyBinance(freeBalance, marketConfigs, ask, currency, priceFilter, pair) {
    var fiatAmount = 0;
    if (freeBalance < marketConfigs.minValueSellBUSD * 2) {
        console.log("however i can create only 1 order, spend the entire balance");
        fiatAmount = freeBalance;
    } else if (freeBalance * marketConfigs.balanceCutForSpendCripto < marketConfigs.minValueSellBUSD) {
        console.log("however the market cut configured doesnt match the minimum amount of Binance 10 BUSD, putting the minimum order value");
        fiatAmount = marketConfigs.minValueSellBUSD;
    } else {
        fiatAmount = freeBalance * marketConfigs.balanceCutForSpendCripto;
    }
    var priceToBuy = ask - (ask * marketConfigs.percentagePriceCalc);
    console.log(`ask price for ${currency} is ${ask}`);
    var formattedPrice = toFixedNotRound(priceToBuy, 1);
    console.log(formattedPrice);
    if (formattedPrice == ask) {
        formattedPrice = formattedPrice - priceFilter.tickSize;
        var priceComposition = (formattedPrice + "").split(".");
        formattedPrice = `${priceComposition[0]}.${priceComposition[1].substring(0, 3)}`;
    }
    var calc = (fiatAmount / formattedPrice + "").split(".");
    calc = `${calc[0]}.${calc[1].substring(0, 3)}`;
    console.log(calc);
    console.log(`spend ${fiatAmount} buying ${currency} which totalizes in a buy order of ${calc} BCH priced at ${formattedPrice}`);
    //await binanceApi.placeOrder(pair,"BUY","LIMIT",formattedPrice,calc)
    mysql.createOrder(calc, formattedPrice, "buy", pair, null, "BINANCE", "Normal market buy order", null);
}

function toFixedNotRound(number, decimals) {
    var x = Math.pow(10, Number(decimals) + 1);
    return (Number(number) + (1 / x)).toFixed(decimals)
}
async function checkAndCancelBinanceOrders(pair, bid, ask, orderDisparity) {
    var openOrders = await binanceApi.getOpenOrders(pair)
    for (let o of openOrders) {
        var orderDetails = await binanceApi.getOpenOrder(pair, o.orderId)
        var price = new Number(orderDetails.price)
        var orderSide = orderDetails.side //BUY,SELL
        if (orderSide == "BUY") {
            //compare to bid
            console.log(`Is ${price * orderDisparity + price} lower than ${bid}?`)
            if ((price * orderDisparity + price) < bid) {
                console.log("yes")
                binanceApi.cancelOrder(pair, o.orderId);
                mysql.createOrder(o.origQty, price, o.side, pair, 0, "BINANCE", "Cancel, market price too low for buy", o.orderId)
            }
        } else if (orderSide == "SELL") {
            //compare to ask
            console.log(`Is ${price - (price * orderDisparity)} higher than ${ask}?`);
            if (price - (price * orderDisparity) > highestaskPrice) {
                console.log("yes")
                binanceApi.cancelOrder(pair, o.orderId);
                mysql.createOrder(o.origQty, price, o.side, pair, 0, "BINANCE", "Cancel, market price too low for sell", o.orderId)
            }
        }
    }
}

//Mercado bitcoin script
async function runMbtcBlock() {
    console.log("Running Mercado Bitcoin block...")
    const accounts = await mbtcTradeApi.listAccounts()
    var balances = await mbtcTradeApi.listBalance(accounts[0].id)
    for (b of balances) {
        balancesObj[b.symbol] = b
    }
    await utils.sleep(1000)
    // Market Margins variables
    console.log("Loading spefic currency info...")
    var moedas = ["BCH"]
    const minBrlOrder = 1
    var minValueSell = {
        "LTC": "0.001",
        "XRP": "0.1",
        "BCH": "0.002",
        "ETH": "0.001",
    }
    const openOrders = await mbtcTradeApi.listPositions(accounts[0].id, moedas)
    // Market cut (percentage) - Do not operate if Lowest trade differs (positively or negatively) from the current trade price in the last 24h
    var spreadCut = 6
    // Percentage price calc - the amount of % higher or lower to buy or sell assets given their last order (mbtc takes 0.7% cut - equivalent to a minimum of 0.007 in this field)
    var percentagePriceCalc = 0.006 // 0.6% - Mercado bitcoin taxes are 0.7% taker 0.3% maker - this is a 0.3% margin for each trade
    // Balance cut spend for fiat - the amount of fiat (BRL) % the script should spend in each run, for a specific ticker, this value is based on the maximum balance, not the available balance
    // It is desireable to spend less % if you have a bigger balance
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

        await checkMarketSpread(spreadPercentage, spreadCut, accounts, m, balanceCutForSpendFiat, highesbidPrice, percentagePriceCalc, balanceCutForSpendCripto, minValueSell, minBrlOrder, highestaskPrice);


        console.log("-------------");
        var balances = await mbtcTradeApi.listBalance(accounts[0].id)
        for (b of balances) {
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
            if ((price * orderDisparity + price) < highesbidPrice) {
                console.log('buy order much lower than market value, should cancel');
                mbtcTradeApi.cancelOrder(accounts[0].id, o.instrument, o.id);
                mysql.createOrder(o.qty, price, o.side, pair, 0, "MBTC", "Cancel, market price too low for buy", o.id)
            }
        } else {
            console.log(`is ${price - (price * orderDisparity)} bigger than ${highestaskPrice} ?`)
            if (price - (price * orderDisparity) > highestaskPrice) {
                console.log('sell order much higher than market value, should cancel');
                mbtcTradeApi.cancelOrder(accounts[0].id, o.instrument, o.id);
                mysql.createOrder(o.qty, price, o.side, pair, 0, "MBTC", "Cancel, market price too low for sell", o.id)
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

        await checkBuy(balanceCutForSpendFiat, percentagePriceCalc, m, highestaskPrice, minValueSell, minBrlOrder, accounts);

        await checkSell(highesbidPrice, percentagePriceCalc, m, balanceCutForSpendCripto, minValueSell, minBrlOrder, accounts);
        utils.sleep(1000);
    } else {
        console.log(`Market spread is higher than 3%, dont buy or sell`);
    }
}
var resultBuyQuery
async function checkSell(highesbidPrice, percentagePriceCalc, m, balanceCutForSpendCripto, minValueSell, minBrlOrder, accounts) {
    const sellPrice = highesbidPrice + (highesbidPrice * percentagePriceCalc);
    const criptoBalance = balancesObj[m];
    const availableCriptoCurrentToSpend = (criptoBalance.total * balanceCutForSpendCripto);
    if (availableCriptoCurrentToSpend > minValueSell[m] &&
        ((availableCriptoCurrentToSpend * sellPrice) > minBrlOrder)) {
        console.log(`Current ${m} allocation balance to sell is higher than available balance, create sell order"`);
        var latestBuyOrderInThisTicker = []
        await mysql.lastOrder(m, "MBTC", "buy", function (err, content) {
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
            if (latestBuyOrderInThisTicker.length < 1) {
                //create order
                mysql.createOrder(availableCriptoCurrencyToSpend, sellPrice, "sell", `${m}-BRL`, null, "MBTC", "Selling based on market value", null)
                mbtcTradeApi.placeOrder(accounts[0].id, `${m}-BRL`, null, sellPrice, availableCriptoCurrencyToSpend, "sell", "limit")
            } else {
                var basedOnResult = []
                await mysql.basedOnOrder(latestBuyOrderInThisTicker[0].id, function (err, content) {
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
                    mysql.createOrder(availableCriptoCurrencyToSpend, sellPrice, "sell", `${m}-BRL`, null, "MBTC", "Selling based on market value", null)
                    mbtcTradeApi.placeOrder(accounts[0].id, `${m}-BRL`, null, sellPrice, availableCriptoCurrencyToSpend, "sell", "limit")
                } else {
                    var price = (latestBuyOrderInThisTicker[0].price * percentagePriceCalc) + new Number(latestBuyOrderInThisTicker[0].price)
                    console.log(highesbidPrice)
                    if (price < highesbidPrice) {
                        console.log("calculated price is lower than current market, set price to market value")
                        price = highesbidPrice
                    }
                    mysql.createOrder(availableCriptoCurrencyToSpend, price, "sell", `${m}-BRL`, latestBuyOrderInThisTicker[0].id, "MBTC", "creating order based on last order", null)
                    mbtcTradeApi.placeOrder(accounts[0].id, `${m}-BRL`, null, price, availableCriptoCurrencyToSpend, "sell", "limit")
                }
            }
        }
    } else {
        console.log("Not enough balance to sell");
    }
}
var resultQuery
async function checkBuy(balanceCutForSpendFiat, percentagePriceCalc, m, highestaskPrice, minValueSell, minBrlOrder, accounts) {
    const balanceObjFiat = balancesObj["BRL"];
    var latestBuyOrderInThisTicker = []
    await mysql.lastOrder(m, "MBTC", "sell", function (err, content) {
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
        if (latestBuyOrderInThisTicker.length < 1) {
            const price = highestaskPrice - (highestaskPrice * percentagePriceCalc)
            const amount = availableFiatCurrencyToSpend / price
            if (amount > minValueSell[m]) {
                //create order
                mysql.createOrder(amount, price, "buy", `${m}-BRL`, null, "MBTC", "lower than highestbid normal buy order", null)
                mbtcTradeApi.placeOrder(accounts[0].id, `${m}-BRL`, null, price, amount, "buy", "limit")
            }
        } else {
            var basedOnResult = undefined
            await mysql.basedOnOrder(latestBuyOrderInThisTicker[0].id, function (err, content) {
                if (err) {
                    console.log(err);
                    // Do something with your error...
                } else {
                    basedOnResult = content
                }
            });
            await utils.sleep(50)
            if (basedOnResult.length > 0) {
                const price = highestaskPrice - (highestaskPrice * percentagePriceCalc)
                const amount = availableFiatCurrencyToSpend / price
                mysql.createOrder(amount, price, "buy", `${m}-BRL`, null, "MBTC", "sell order already used, create based on market value", null)
                mbtcTradeApi.placeOrder(accounts[0].id, `${m}-BRL`, null, price, amount, "buy", "limit")
            } else {
                var price = new Number(latestBuyOrderInThisTicker[0].price) - (latestBuyOrderInThisTicker[0].price * percentagePriceCalc)
                const amount2 = availableFiatCurrencyToSpend / price
                if (price > highestaskPrice) {
                    //console.log("calculated price is lower than current market, set price to market value")
                    price = highestaskPrice
                }
                mysql.createOrder(amount2, price, "buy", `${m}-BRL`, latestBuyOrderInThisTicker[0].id, "MBTC", "creating order based on last order", null)
                mbtcTradeApi.placeOrder(accounts[0].id, `${m}-BRL`, null, price, amount2, "buy", "limit")

            }
            utils.sleep(10000)
        }
    } else {
        console.log("Not enough balance to buy");
    }
}

