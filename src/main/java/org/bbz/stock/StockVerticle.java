package org.bbz.stock;

import io.vertx.core.*;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import org.bbz.stock.pojo.StockInfo;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private MailClient mailClient;

    private StockInfo stockInfo = new StockInfo();
    private MailMessage mail;
    //低于此数值就报警
    private float alert;

    @Override
    public void start(){
        init();


        httpClient = vertx.createHttpClient();

        vertx.setPeriodic( 60 * 1000, this::run );
    }

    private void init(){
        Map<String, Integer> stocksMap = new HashMap<>();
        String[] stocks = config().getString( "stocks" ).split( "\\|" );
        for( String stock : stocks ) {
            String[] stockPair = stock.split( "," );
            stocksMap.put( stockPair[0], Integer.parseInt( stockPair[1] ) );

        }
        log.info( stocksMap.toString() );


        stockInfo.setStocksMap( stocksMap );
        stockInfo.setCash( config().getFloat( "cash" ) );
        alert = config().getFloat( "alert" );

        MailConfig mailConfig = new MailConfig();
        mailConfig.setHostname( "smtp.qq.com" );
        mailConfig.setPort( 465 );
//        mailConfig.setStarttls( StartTLSOptions.REQUIRED );
        mailConfig.setSsl( true );
        String mailSender = config().getString( "mail_user" );
        mailConfig.setUsername( mailSender );
        mailConfig.setPassword( config().getString( "mail_pass" ) );

        mailClient = MailClient.createShared( vertx, mailConfig );

        mail = new MailMessage();
        mail.setFrom( mailSender );
        mail.setTo( config().getString( "mail_recv" ) );
//        mail.setCc("Another User <another@example.net>");
        mail.setText( "报警" );
        mail.setSubject( "前方高能" );
    }

    @SuppressWarnings("unused")
    private void run( Long aLong ){
        List<Future> futures = new ArrayList<>();
        for( String stockId : stockInfo.getStocksMap().keySet() ) {
            futures.add( getStockPrice( stockId ) );
        }


        CompositeFuture.join( futures ).setHandler( f -> {
            float worth = 0f;
            final List<Float> list = f.result().list();
            for( float v : list ) {
                worth += v;
            }
            String text = "股票市值【"+worth + "】 + 现金【" + stockInfo.getCash() + "】 = " + (worth + stockInfo.getCash()) + "<" + alert;
            log.info( text );
            if( worth + stockInfo.getCash() < alert ) {
                alert(text);
            }
        } );
    }

    /**
     * 股票市值加上现金低于报警值，发邮件报警
     * @param text
     */
    private void alert( String text ){
        log.info( "开始发送邮件" );
        mail.setText( text );
        mailClient.sendMail( mail, res -> {
            if( res.succeeded() ) {
                log.info( res.result().toString() );
            } else {
                res.cause().printStackTrace();
            }
            mail.setText( "" );

        } );
    }

    private Future<Float> getStockPrice( String stockId ){
        final Future<Float> future = Future.future();
        String requestUrl = "/q=s_" + stockId;
        httpClient.getNow( 80, "qt.gtimg.cn", requestUrl, response -> response.bodyHandler( body -> {
            String resp = body.toString( Charset.forName( "gb2312" ) );
//                System.out.println( resp );
            future.complete( parseResponse( resp ) * stockInfo.getStocksMap().get( stockId ) );
        } ) );
        return future;
    }

    private float parseResponse( String response ){
//        log.info( response );
        return Float.parseFloat( response.split( "~" )[3] );
    }

    public static void main( String[] args ) throws IOException{
        final VertxOptions vertxOptions = new VertxOptions();
        vertxOptions.setBlockedThreadCheckInterval( 1000000 );
        Vertx vertx = Vertx.vertx( vertxOptions );

        DeploymentOptions options = new DeploymentOptions();
        options.setInstances( 1 );

        String content = new String( Files.readAllBytes( Paths.get( "resources/application-conf.json" ) ) );
        final JsonObject config = new JsonObject( content );

        log.info( config.toString() );
        options.setConfig( config );

        vertx.deployVerticle( StockVerticle.class.getName(), options, res -> {
            if( res.succeeded() ) {
                log.info( " server started " );
            } else {
                res.cause().printStackTrace();
            }
        } );
    }
}

