class Utils {
    constructor(config) {
        
    }
    async sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
    getTickerInfo(pair,ticker){
        for (let t of ticker){
            if (t.pair == pair){
                return t
            }
        }
    }
    getTickerInfoBinance(pair,ticker){
        for (let t of ticker){
            if (t.symbol == pair){
                return t
            }
        }
    }
}

module.exports = {
    Utils
}