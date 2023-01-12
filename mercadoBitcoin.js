//MERCADO BITCOIN
const axios = require('axios');
const qs = require('querystring')
const crypto = require('crypto')

const ENDPOINT_API = 'https://www.mercadobitcoin.net/api/';
const api_path = '/api/v4'
const TRADE_API = 'https://api.mercadobitcoin.net' + api_path

class MercadoBitcoinTrade {
    constructor(config) {
        this.config = {
            CURRENCY: config.currency,
            KEY: config.key,
            SECRET: config.secret,
        }
    }

    placeBuyOrder(qty,limit_price){
        
    }

    listAccounts(){
        return this.call(`${TRADE_API}/accounts`);
    }

    async call(method, params){
        const now = new Date().getTime();
        let querystring = qs.stringify({tapi_method: method, tapi_nonce: now})
        if (params) querystring = querystring += `&${qs.stringify(parameters)}`

        const sig = crypto.createHmac('sha512', this.config.SECRET).update(`${TRADE_API}?${querystring}`).digest( 'hex' )

        const config = {
            headers: {
                'TAPI-ID': this.config.KEY,
                'TAPI-HMAC': sig
            }
        }
        const response = await axios.post(TRADE_API, querystring, config )
        if (response.data.error_message) throw new Error(response.data.error_message)
        return response.data
    }

}

class MercadoBitcoin {

    constructor(config) {
        this.config = {
            CURRENCY: config.currency
        }
    }

    ticker() {
        return this.call(`${ENDPOINT_API}${this.config.CURRENCY}/ticker`);
    }

    orderBook() {
        return this.call(`${ENDPOINT_API}${this.config.CURRENCY}/orderbook`);
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
    MercadoBitcoin, MercadoBitcoinTrade
}