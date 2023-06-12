var mysql = require('mysql2');
require("dotenv-safe").config()
const con = mysql.createConnection({
    host: process.env.HOST,
    port: process.env.PORT,
    user: process.env.USER,
    password: process.env.PASSWORD
});



class Mysql{
    constructor(config) {
        
    }
    connect () {
        con.connect(function(err) {
            if (err) throw err;
            console.log("Connected to MYSQL!");
        });
    }
    async lastOrder(currency,market, side,callback ){
        var sql = "SELECT id, qty, price, side, ticker, based_on_order, market, reason, market_id, `time` FROM tradebot.orders ";
        var where =  `where based_on_order is null and ticker like '%${currency}%' and market like '${market}' and side = '${side}' order by time desc`  
        var rowsRet = []
        con.query(sql + where, function(err,rows,fields){
            if (err) {
                callback(err, null);
            } else 
                callback(null, rows);
        })
        return rowsRet

    }
    async lastOrderWithoutBased(currency,market, side,callback ){
        var sql = "SELECT id, qty, price, side, ticker, based_on_order, market, reason, market_id, `time` FROM tradebot.orders ";
        var where =  `where ticker like '%${currency}%' and market like '${market}' and side = '${side}' order by time desc`  
        var rowsRet = []
        con.query(sql + where, function(err,rows,fields){
            if (err) {
                callback(err, null);
            } else 
                callback(null, rows);
        })
        return rowsRet

    }
    basedOnOrder(id,callback ){
        var sql = "SELECT id, qty, price, side, ticker, based_on_order, market, reason, market_id, `time` FROM tradebot.orders ";
        var where =  `where based_on_order = ${id} order by time desc`   
        con.query(sql + where, function(err,rows,fields){
            if (err) {
                callback(err, null);
            } else 
                callback(null, rows);
        })    
    }
    createOrder(qty,price,side,ticker,based_on_order,market,reason,market_id){
        var sql = `INSERT INTO tradebot.orders (qty, price, side, ticker, based_on_order,market,reason,market_id,time) VALUES('${qty}', '${price}', '${side}', '${ticker}', ${based_on_order}, '${market}', '${reason}', '${market_id}', '${new Date().getTime()}');`
        var query = con.execute(sql, function(err, rows, fields){
            if (err) {
                console.log(err);
            }
            else {
                return rows;
            }
        })
    }
}

module.exports = {
    Mysql
}