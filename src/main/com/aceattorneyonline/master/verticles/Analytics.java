package com.aceattorneyonline.master.verticles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aceattorneyonline.master.MasterServer;
import com.aceattorneyonline.master.events.Events;
import com.aceattorneyonline.master.events.SharedEventProtos.AnalyticsEvent;
import com.brsanthu.googleanalytics.EventHit;
import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.google.protobuf.InvalidProtocolBufferException;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

public class Analytics extends AbstractVerticle {
	
	private static final Logger logger = LoggerFactory.getLogger(Analytics.class);

	private static final String APPLICATION_NAME = "AOMasterServer";
	private static final String TRACKING_ID = "UA-102422742-1";
	// private static final String VIEW_ID = "167900174";
	private GoogleAnalytics analytics;

	@Override
	public void start() {
		logger.info("Analytics verticle starting");
		analytics = new GoogleAnalytics(TRACKING_ID, APPLICATION_NAME, MasterServer.VERSION);
		EventBus eventBus = getVertx().eventBus();
		eventBus.consumer(Events.PLAYER_CONNECTED.toString(), this::handlePlayerConnected);
		eventBus.consumer(Events.ADVERTISER_CONNECTED.toString(), this::handleAdvertiserConnected);
		eventBus.consumer(Events.PLAYER_LEFT.getEventName(), this::handlePlayerLeft);
		eventBus.consumer(Events.ADVERTISER_LEFT.getEventName(), this::handleAdvertiserLeft);
	}
	
	@Override
	public void stop() {
		logger.info("Analytics verticle stopping");
	}
	
	private void handlePlayerConnected(Message<byte[]> event) {
		try {
			AnalyticsEvent analyticsEvent = AnalyticsEvent.parseFrom(event.body());
			EventHit hit = createEvent(analyticsEvent)
					.eventCategory("ms_player")
					.eventAction("connected");
			analytics.postAsync(hit);
		} catch (InvalidProtocolBufferException e) {
			logger.error("Error handling player connected event", e);
		}
	}
	
	private void handleAdvertiserConnected(Message<byte[]> event) {
		try {
			AnalyticsEvent analyticsEvent = AnalyticsEvent.parseFrom(event.body());
			EventHit hit = createEvent(analyticsEvent)
					.eventCategory("ms_advertiser")
					.eventAction("connected");
			analytics.postAsync(hit);
		} catch (InvalidProtocolBufferException e) {
			logger.error("Error handling advertiser connected event", e);
		}
	}
	
	private void handlePlayerLeft(Message<byte[]> event) {
		try {
			AnalyticsEvent analyticsEvent = AnalyticsEvent.parseFrom(event.body());
			EventHit hit = createEvent(analyticsEvent)
					.eventCategory("ms_player")
					.eventAction("left");
			analytics.postAsync(hit);
		} catch (InvalidProtocolBufferException e) {
			logger.error("Error handling player left event", e);
		}
	}
	
	private void handleAdvertiserLeft(Message<byte[]> event) {
		try {
			AnalyticsEvent analyticsEvent = AnalyticsEvent.parseFrom(event.body());
			EventHit hit = createEvent(analyticsEvent)
					.eventCategory("ms_advertiser")
					.eventAction("left");
			analytics.postAsync(hit);
		} catch (InvalidProtocolBufferException e) {
			logger.error("Error handling advertiser left event", e);
		}
	}
	
	private EventHit createEvent(AnalyticsEvent event) {
		EventHit hit = new EventHit()
				.userIp(event.getAddress());
		
		if (event.getVersion() != null) {
			hit.userAgent(event.getVersion());
		}
		
		// AO1 does not populate the ID because the AO1 client
		// does not send a hardware ID. Instead, we will use the
		// IP address once more.
		if (event.getId() != null) {
			hit.userId(event.getId());
		} else {
			hit.userId(event.getAddress());
		}
		
		return hit;
	}

}
