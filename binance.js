//MERCADO BITCOIN
const axios = require('axios');

const ENDPOINT_API = 'https://api1.binance.com';

class Binance {

    constructor(config) {
        this.config = {
            
        }
    }

    ping() {
        return this.call(`${ENDPOINT_API}/api/v3/ping`);
    }

    orderBook() {
        return this.call('orderbook');
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