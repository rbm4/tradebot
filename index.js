require("dotenv-safe").config()
const { MercadoBitcoin, MercadoBitcoinTrade } = require("./mercadoBitcoin");
const { Binance } = require("./binance");
var infoApi = new MercadoBitcoin({ currency: 'XRP' });
var mbtcTradeApi = new MercadoBitcoinTrade({
    currency: 'XRP',
    key: process.env.KEY,
    secret: process.env.SECRET,
})
var binanceApi = new Binance({currency: 'XRP'})
var balancesObj = {}
var ticker = {}
function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}
function getTickerInfo(pair){
    for (let t of ticker){
        if (t.pair == pair){
            return t
        }
    }
}
setTimeout(async () => {
    console.log("Carregando informações de saldo...")
    const accounts = await mbtcTradeApi.listAccounts()
    const balances = await mbtcTradeApi.listBalance(accounts[0].id)
    for (b of balances){
        balancesObj[b.symbol] = b
    }
    sleep(1000)
    console.log("Carregando informações de ordens para as moedas específicas...")
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
    for (let m of moedas){
        let pair = `${m}-BRL`
        let tickerInfo = getTickerInfo(pair)
        let high = tickerInfo.high
        let low = tickerInfo.low
        let current = tickerInfo.last
        let buy = tickerInfo.buy
        let sell = tickerInfo.sell
        let spreadPercentage = 100-((100*tickerInfo.low) / tickerInfo.last)
        console.log(`Pair: ${pair}`)
        console.log(`High: ${high} Low: ${low} Current: ${current} Buy: ${buy} Sell ${sell}`)
        console.log(`SpreadPercentage: ${spreadPercentage}`)
        if (spreadPercentage <= spreadCut && spreadPercentage >= (spreadCut * -1)){
            console.log(`Market spread less than ${spreadCut}, and higher than ${(spreadCut * -1)} check market prices and currency allocation`)
            //allocate 25% of current balance to operate
            sleep(1000)
            await mbtcTradeApi.listOrders(accounts[0].id,m)
            const book = await infoApi.orderBook(m)
            const highestask = book.asks[0]
            const highestBid = book.bids[0]
            const highestaskPrice = highestask[0]
            const highestaskVolume = highestask[1]
            const highesbidPrice = highestBid[0]
            const highesbidVolume = highestBid[1]
            const balanceObjFiat = balancesObj["BRL"]
            const availableFiatCurrencyToSpend = (balanceObjFiat.total * balanceCutForSpendFiat)
            if (availableFiatCurrencyToSpend < balanceObjFiat.available){
                console.log("Current fiat allocation balance to buy is higher than available balance, create buy order")
            } else {
                console.log("Not enough balance to buy")
            }

            const sellPrice = highesbidPrice + (highesbidPrice * percentagePriceCalc)
            const criptoBalance = balancesObj[m]
            const availableCriptoCurrentToSpend = (criptoBalance.total * balanceCutForSpendCripto)
            if ((availableCriptoCurrentToSpend < criptoBalance.available) &&
             availableCriptoCurrentToSpend > minValueSell[m] && 
             ((availableCriptoCurrentToSpend * sellPrice) > minBrlOrder)){
                console.log(`Current ${m} allocation balance to sell is higher than available balance, create sell order"`)
            } else {
                console.log("Not enough balance to sell")
            }
            console.log("verify market price and open orders, if they dont meet conditions, cancel then")
            const ordersFromThisTicker = []
            for (let o of openOrders){
                if (o.instrument == pair){
                    ordersFromThisTicker.concat(o)
                }
            }
            for (let openOrders of ordersFromThisTicker){
                console.log(openOrders)
            }
            console.log("-------------")
        }
        
    }
    
},
    process.env.CRAWLER_INTERVAL
)