package com.companyx.service;

import com.companyx.domain.AppMessage;
import com.companyx.domain.AppMessageResponse;

import java.util.concurrent.Future;


/**
 * A business interface used to send messages via Spring Integration.
 */
public interface AppMessageServiceAsync {

    Future<AppMessageResponse> sendMessage(AppMessage appMessage);

}
