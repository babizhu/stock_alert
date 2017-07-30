package org.bbz.stock;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by liulaoye on 17-6-16.
 * 看板监控
 */


public class DashboardVerticle extends AbstractVerticle{
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( DashboardVerticle.class );
    private HttpClient httpClient;
    private MailClient mailClient;
    private int port;
    private String host;
    private String uri;

    //连续多少次连不上就报警
    private float errorCount = 0;

    //    private StockInfo stockInfo = new StockInfo();
    private MailMessage mail;
    //连续多少次连不上就报警
    private float currentErrorCount = 0;

    @Override
    public void start(){
        init();


        httpClient = vertx.createHttpClient();
//        httpClient.options( config().getString( "host" ));
        vertx.setPeriodic( 10 * 1000, this::run );
    }

    private void init(){
        errorCount = config().getInteger( "error_count", 3 );
        port = config().getInteger( "port", 80 );
        host = config().getString( "host" );
        uri = config().getString( "uri" );

        MailConfig mailConfig = new MailConfig();
        mailConfig.setHostname( "smtp.qq.com" );
        mailConfig.setPort( 465 );
        mailConfig.setSsl( true );
        String mailSender = config().getString( "mail_user" );
        mailConfig.setUsername( mailSender );
        mailConfig.setPassword( config().getString( "mail_pass" ) );

        mailClient = MailClient.createShared( vertx, mailConfig );

        mail = new MailMessage();
        mail.setFrom( mailSender );
        mail.setTo( config().getString( "mail_recv" ) );
        mail.setCc( config().getString( "mail_cc" ) );
        mail.setText( "报警" );
        mail.setSubject( "前方高能" );
    }

    @SuppressWarnings("unused")
    private void run( Long aLong ){
        httpClient.post( port, host, uri, event -> event.bodyHandler( body -> {
            if( event.statusCode() != 200 ) {
                if( currentErrorCount++ > errorCount ) {
                    alert( "500错误" );
                    currentErrorCount = 0;
                }
            } else {
                System.out.println( body );
                errorCount = 0;
            }
        } ) )
                .exceptionHandler( ex -> {
                    System.out.println( ex.getMessage() );
                    if( currentErrorCount++ > errorCount ) {
                        alert( ex.getMessage() );
                        currentErrorCount = 0;
                    }
                } )
                .end();

//        try {
//
//
//        }catch( Exception exception ){
//            System.out.println( exception.getCause());
//            if(currentErrorCount++ > errorCount){
//                System.out.println( "发送邮件" );
//                currentErrorCount = 0;
//            }
//
//        }
    }

    /**
     * 连续多少次无法访问web地址，发邮件报警
     *
     * @param text
     */
    private void alert( String text ){
        log.info( "开始发送邮件" );
        mail.setText( text );
        mailClient.sendMail( mail, res -> {
            if( res.succeeded() ) {
                log.info( res.result().toString() + " send success!" );
            } else {
                res.cause().printStackTrace();
            }
            mail.setText( "" );

        } );
    }


    public static void main( String[] args ) throws IOException{
        final VertxOptions vertxOptions = new VertxOptions();
        vertxOptions.setBlockedThreadCheckInterval( 1000000 );
        Vertx vertx = Vertx.vertx( vertxOptions );

        DeploymentOptions options = new DeploymentOptions();
        options.setInstances( 1 );

        String content = new String( Files.readAllBytes( Paths.get( "resources/application-conf1.json" ) ) );
        final JsonObject config = new JsonObject( content );

        log.info( config.toString() );
        options.setConfig( config );

        vertx.deployVerticle( DashboardVerticle.class.getName(), options, res -> {
            if( res.succeeded() ) {
                log.info( " server started " );
            } else {
                res.cause().printStackTrace();
            }
        } );
    }
}

