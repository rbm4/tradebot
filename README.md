# Tradebot
Bot de trade em node.js para uso em exchanges. A utilização de Bots para Trading oferece algumas vantagens, baseado na raiz da velocidade da tomada de decisão, permitindo relizar operações que um humano comum não conseguiria, buscando a margem mínima de lucro e realizando operações em escala de compra e venda. 

Este não é um projeto de machine learning, mas apenas uma aplicação básica de regras e funções integradas realizando operações de compra e venda com margens específicas para cada ticker

## Features
- Integração com o Mercado bitcoin, onde é necessário preencher os dados no arquivo .env conforme o formato .env.example
- Listagem de ticker, listagem de ordens, listagem de saldo, listagem de contas, entre outras funções
- Execução, por meio de script, no arquivo index.js, 

## Como usar
- Necessário node.js e npm
- Necessário chaves de API (https://api.mercadobitcoin.net/api/v4/docs) - (https://binance-docs.github.io/apidocs/spot/en/#introduction)
- Node (https://nodejs.org/en/download/) e npm
- Configurar as margens de mercado:
```
    // Market cut (percentage) - Do not operate if Lowest trade differs (positively or negatively) from the current trade price in the last 24h
    var spreadCut = 3
    // Percentage price calc - the amount of % higher or lower to buy or sell assets given their last order (mbtc takes 0.7% cut - equivalent to a minimum of 0.007 in this field)
    var percentagePriceCalc = 0.0095 // 0.95% - Mercado bitcoin taxes are 0.7% - this is a 0.25% margin for each trade
    // Balance cut spend for fiat - the amount of fiat (BRL) % the script should spend in each run, for a specific ticker, this value is based on the maximum balance, not the available balance
    // It is desireable to spend less % if you have a bigger balance
    var balanceCutForSpendFiat = 0.25
    var balanceCutForSpendCripto = 0.25
```

    
```
npm install
node index.js
```


## Features futuras
- Implementação com Binance

# Tradebot
Trading bot in node.js for use in Mercado bitcoin (https://www.mercadobitcoin.com.br/)
- Todo insert english description
