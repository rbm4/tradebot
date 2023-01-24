const { Spot } = require('@binance/connector')




// Place a new order


class Binance {
    client = {}

    constructor(config) {
        this.client = new Spot(config.key, config.secret)
    }

    ping() {
        return this.call(`${ENDPOINT_API}/api/v3/ping`);
    }
    exchangeInfo(symbols){
        var tickers = []
        for (let m of symbols){
            tickers.push(`${m}BUSD`)
        }
        return this.client.exchangeInfo({ symbols: tickers }).then(response => {
            return response.data
        }).catch(e => this.client.logger.error(e))
    }
    coinInfo(){
        return this.client.coinInfo().then(response => {
            return response.data
        }).catch(e => this.client.logger.error(e))
    }
    getOpenOrders(pair){
        return this.client.openOrders({ symbol: pair }).then(response => {
            return response.data
        })
    }
    getOpenOrder(pair,id){
        return this.client.getOrder(pair, {
            orderId: id
          }).then(response => {
            return response.data
          })
    }
    cancelOrder(pair,id){
        return this.client.cancelOrder(pair, {
            orderId: id
          }).then(response => {
            return response.data
          }).catch(error => this.client.logger.error(error))
    }
    accountInfo() {
        return this.client.account().then(response => {
            return response.data
        })
    }

    tickers(moedas){
        var tickers = []
        for (let m of moedas){
            tickers.push(`${m}BUSD`)
        }
        return this.client.bookTicker("",tickers).then(response => {
            return response.data
        })
    }

    placeOrder(ticker,side,type,price,amount){
        return this.client.newOrder(ticker, side, type, {
            price: price,
            quantity: amount.toFixed(8) + "",
            timeInForce: 'GTC'
          }).then(response => {return response.data})
            .catch(error => this.client.logger.error(error))
    }

    async call(url) {

        let config = {
            headers: {
                'Accept': 'application/json',
            }
        }

        try {
            const response = await axios.get(url, config);
            return response.data;
        } catch (error) {
            console.error(error);
            return false;
        }
    }
}

module.exports = {
    Binance
}