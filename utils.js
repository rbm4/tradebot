class Utils {
    async sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
    getTickerInfo(pair){
        for (let t of ticker){
            if (t.pair == pair){
                return t
            }
        }
    }
}

module.exports = {
    Utils
}