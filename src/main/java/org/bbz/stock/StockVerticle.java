package org.bbz.stock;

import io.vertx.core.*;
import io.vertx.core.http.HttpClient;
import org.bbz.stock.pojo.StockInfo;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by liulaoye on 17-6-16.
 * 股票监控
 */


public class StockVerticle extends AbstractVerticle{
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( StockVerticle.class );
    private HttpClient httpClient;

    private StockInfo stockInfo = new StockInfo();

    @Override
    public void start(){
        Map<String, Integer> stocksMap = new HashMap<>();
        stocksMap.put( "sh600888", 100 );
        stocksMap.put( "sh600887", 200 );
        stockInfo.setStocksMap( stocksMap );

        httpClient = vertx.createHttpClient();

        vertx.setPeriodic( 10000, this::run );
    }

    private void run( Long aLong ){
        List<Future> futures = new ArrayList<>();
        for( String stockId : stockInfo.getStocksMap().keySet() ) {
            futures.add( getStockPrice( stockId ) );
        }


        CompositeFuture.join( futures ).setHandler( f -> {
            float worth = 0f;
            final List<Float> list = f.result().list();
            for( float v : list ) {
                worth += v ;
            }

            System.out.println("总价值 ：" + worth);
        } );
    }

    private Future<Float> getStockPrice( String stockId ){
        final Future<Float> future = Future.future();
        String requestUrl = "/q=s_" + stockId;
        httpClient.getNow( 80, "qt.gtimg.cn", requestUrl, response -> {
            response.bodyHandler( body -> {
                String resp = body.toString( Charset.forName( "gb2312" ) );
//                System.out.println( resp );
                future.complete( parseResponse( resp ) * stockInfo.getStocksMap().get( stockId ) );
            } );
        } );
        return future;
    }

    private float parseResponse( String response ){
        return Float.parseFloat( response.split( "~" )[3] );
    }

    public static void main( String[] args ){
        final VertxOptions vertxOptions = new VertxOptions();
        vertxOptions.setBlockedThreadCheckInterval( 1000000 );
        Vertx vertx = Vertx.vertx( vertxOptions );

        DeploymentOptions options = new DeploymentOptions();
        options.setInstances( 1 );

        vertx.deployVerticle( StockVerticle.class.getName(), options, res -> {
            if( res.succeeded() ) {
                log.info( " server started " );
            } else {
                res.cause().printStackTrace();
            }
        } );
    }
}

