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
 
setInterval(async () => {
    const book = await infoApi.orderBook()
    const highestask = book.asks[0]
    const highestBid = book.bids[0]
    console.log("preço de compra / qtd")
    console.log(highestask);
    console.log("preço de venda / qtd")
    console.log(highestBid);
    console.log("-------------")
    const accounts = mbtcTradeApi.listAccounts()
},
    process.env.CRAWLER_INTERVAL
)