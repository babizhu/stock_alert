package org.bbz.stock.pojo;

import lombok.Data;

import java.util.Map;

/**
 * Created by liulaoye on 17-6-16.
 */

@Data
public class StockInfo{
    Map<String, Integer>    stocksMap;
    int                     cash;

//    public int getAllWorth(){
//        int worth = 0;
//        for( Integer cash : stocksMap.values() ) {
//            worth += cash;
//        }
//        worth += cash;
//        return worth;
//    }
}
