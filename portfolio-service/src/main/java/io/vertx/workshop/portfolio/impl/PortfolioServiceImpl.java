package io.vertx.workshop.portfolio.impl;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import io.vertx.reactivex.servicediscovery.types.HttpEndpoint;
import io.vertx.workshop.portfolio.Portfolio;
import io.vertx.workshop.portfolio.PortfolioService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * The portfolio service implementation.
 */
public class PortfolioServiceImpl implements PortfolioService {

    private final Vertx vertx;
    private final Portfolio portfolio;
    private final ServiceDiscovery discovery;

    public PortfolioServiceImpl(Vertx vertx, ServiceDiscovery discovery, double initialCash) {
        this.vertx = vertx;
        this.portfolio = new Portfolio().setCash(initialCash);
        this.discovery = discovery;
    }

    @Override
    public void getPortfolio(Handler<AsyncResult<Portfolio>> resultHandler) {
        // Call the given handler with a successful Async Result encapsulating the `portfolio` object
        // The async result instance is created using `Future.succeededFuture`

        // TODO: getPortfolio
    }

    private void sendActionOnTheEventBus(String action, int amount, JsonObject quote, int newAmount) {
        // Broadcast a JSON message to the `EVENT_ADDRESS` containing the following keys: "action", "quote", "date"
        // (use System.currentTimeMillis()), "amount" and "owned" (newAmount)

        // TODO: sendActionOnTheEventBus
    }

    @Override
    public void evaluate(Handler<AsyncResult<Double>> resultHandler) {

        Single<WebClient> quotes = HttpEndpoint.rxGetWebClient(discovery, rec -> rec.getName().equals("quote-generator"));

        // TODO: evaluate
    }

    private void computeEvaluation(WebClient webClient, Handler<AsyncResult<Double>> resultHandler) {
        // We need to call the service for each company in which we own shares
        Flowable.fromIterable(portfolio.getShares().entrySet())
            // For each, we retrieve the value
            .flatMapSingle(entry -> getValueForCompany(webClient, entry.getKey(), entry.getValue()))
            // We accumulate the results
            .toList()
            // And compute the sum
            .map(list -> list.stream().mapToDouble(x -> x).sum())
            // We report the result or failure
            .subscribe((sum, err) -> {
                if (err != null) {
                    resultHandler.handle(Future.failedFuture(err));
                } else {
                    resultHandler.handle(Future.succeededFuture(sum));
                }
            });
    }

    private Single<Double> getValueForCompany(WebClient client, String company, int numberOfShares) {
        // TODO: getValueForCompany

    }


    @Override
    public void buy(int amount, JsonObject quote, Handler<AsyncResult<Portfolio>> resultHandler) {
        if (amount <= 0) {
            resultHandler.handle(Future.failedFuture("Cannot buy " + quote.getString("name") + " - the amount must be " +
                "greater than 0"));
            return;
        }

        if (quote.getInteger("shares") < amount) {
            resultHandler.handle(Future.failedFuture("Cannot buy " + amount + " - not enough " +
                "stocks on the market (" + quote.getInteger("shares") + ")"));
            return;
        }

        double price = amount * quote.getDouble("ask");
        String name = quote.getString("name");
        // 1) do we have enough money
        if (portfolio.getCash() >= price) {
            // Yes, buy it
            portfolio.setCash(portfolio.getCash() - price);
            int current = portfolio.getAmount(name);
            int newAmount = current + amount;
            portfolio.getShares().put(name, newAmount);
            sendActionOnTheEventBus("BUY", amount, quote, newAmount);
            resultHandler.handle(Future.succeededFuture(portfolio));
        } else {
            resultHandler.handle(Future.failedFuture("Cannot buy " + amount + " of " + name + " - " + "not enough money, " +
                "need " + price + ", has " + portfolio.getCash()));
        }
    }


    @Override
    public void sell(int amount, JsonObject quote, Handler<AsyncResult<Portfolio>> resultHandler) {
        if (amount <= 0) {
            resultHandler.handle(Future.failedFuture("Cannot sell " + quote.getString("name") + " - the amount must be " +
                "greater than 0"));
            return;
        }

        double price = amount * quote.getDouble("bid");
        String name = quote.getString("name");
        int current = portfolio.getAmount(name);
        // 1) do we have enough stocks
        if (current >= amount) {
            // Yes, sell it
            int newAmount = current - amount;
            if (newAmount == 0) {
                portfolio.getShares().remove(name);
            } else {
                portfolio.getShares().put(name, newAmount);
            }
            portfolio.setCash(portfolio.getCash() + price);
            sendActionOnTheEventBus("SELL", amount, quote, newAmount);
            resultHandler.handle(Future.succeededFuture(portfolio));
        } else {
            resultHandler.handle(Future.failedFuture("Cannot sell " + amount + " of " + name + " - " + "not enough stocks " +
                "in portfolio"));
        }

    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported encoding");
        }
    }


}
