//MERCADO BITCOIN
const axios = require('axios');
const qs = require('querystring')
const crypto = require('crypto')
var FormData = require('form-data');
var data = new FormData();

const ENDPOINT_API = 'https://www.mercadobitcoin.net/api/';
const api_path = '/api/v4'
const TRADE_API = 'https://api.mercadobitcoin.net' + api_path

class MercadoBitcoinTrade {
    constructor(config) {
        this.config = {
            KEY: config.key,
            SECRET: config.secret,
        }
    }

    placeBuyOrder(qty,limit_price){
        
    }

    async listAccounts(){
        return await this.callGet(`${TRADE_API}/accounts`);
    }
    async listBalance(accountId){
        return await this.callGet(`${TRADE_API}/accounts/${accountId}/balances`);
    }
    async listOrders(accountId,symbol){
        return await this.callGet(`${TRADE_API}/accounts/${accountId}/${symbol}-BRL/orders`)
    }
    async listOrder(accountId,symbol,id){
        return await this.callGet(`${TRADE_API}/accounts/${accountId}/${symbol}/orders/${id}`)
    }
    async cancelOrder(accountId,symbol,id){
        return this.simpleCallDelete(`${TRADE_API}/accounts/${accountId}/${symbol}/orders/${id}`)
    }
    async listPositions(accountId,moedas){
        var tickers = ""
        for (let m of moedas){
            tickers += `${m}-BRL,`
        }
        tickers = tickers.slice(0,-1)
        return await this.callGet(`${TRADE_API}/accounts/${accountId}/positions?symbols=${tickers}`)
    }
    async listAllPositions(accountId){
        return await this.callGet(`${TRADE_API}/accounts/${accountId}/positions`)
    }
    async tickers(moedas){
        var tickers = ""
        for (let m of moedas){
            tickers += `${m}-BRL,`
        }
        
        return await this.callGet(`${TRADE_API}/tickers?symbols=${tickers}`)
    }
    async placeOrder(accountId,symbol,externalId,limitPrice,qty,side,type){
        let body = {
            externalId: externalId,
            limitPrice: limitPrice,
            qty: qty.toFixed(8) + "",
            side: side,
            type: type
        }
        return await this.simpleCallPost(`${TRADE_API}/accounts/${accountId}/${symbol}/orders`,body)
    }
    async authorize(){
        const response = await axios.post(`${TRADE_API}/authorize`, {
            login: this.config.KEY,
            password: this.config.SECRET
        } )
        if (response.data.error_message) throw new Error(response.data.error_message)
        return response.data
    }
    
    async callGet(method){
        var token = await this.authorize()
        const config = {
            headers: {
                'Authorization': token.access_token
            }
        }
        const response = await axios.get(method,config)
        if (response.data.error_message) throw new Error(response.data.error_message)
        return response.data
    }
    async callPost(method, params){
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
    async callDelete(method, params){
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
        const response = await axios.delete(TRADE_API, querystring, config )
        if (response.data.code) throw new Error(response.data.code)
        return response.data
    }
    async simpleCallPost(method, params){
        var token = await this.authorize()
        const config = {
            headers: {
                'Authorization': token.access_token
            }
        }
        const response = await axios.post(method, params, config )
        if (response.data.error_message) throw new Error(response.data.error_message)
        return response.data
    }
    async simpleCallDelete(method){
        var token = await this.authorize()
        var config = {
            method: 'delete',
            url: method,
            headers: { 
              'Authorization': 'Bearer ' + token.access_token, 
              ...data.getHeaders()
            },
            data : data
          };
          
        return axios(config);
    }
}

class MercadoBitcoin {

    constructor(config) {
        
    }

    ticker(currency) {
        return this.call(`${ENDPOINT_API}${currency}/ticker`);
    }

    orderBook(currency) {
        return this.call(`${ENDPOINT_API}${currency}/orderbook`);
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