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

    accountInfo() {
        return this.client.account().then(response => {
            return response.data
        })
    }

    tickers(moedas){
        var tickers = []
        for (let m of moedas){
            tickers.push(`${m}USDT`)
        }
        return this.client.bookTicker("",tickers).then(response => {
            return response.data
        })
    }

    placeOrder(ticker,side,type,price,amount){
        client.newOrder(ticker, side, type, {
            price: price,
            quantity: amount,
            timeInForce: 'GTC'
          }).then(response => client.logger.log(response.data))
            .catch(error => client.logger.error(error))
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