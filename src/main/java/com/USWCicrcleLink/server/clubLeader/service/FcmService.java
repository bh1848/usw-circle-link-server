package com.USWCicrcleLink.server.clubLeader.service;

import com.USWCicrcleLink.server.clubApplication.domain.ClubApplication;
import com.USWCicrcleLink.server.clubApplication.domain.ClubApplicationStatus;
import com.USWCicrcleLink.server.clubLeader.dto.FcmTokenRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public interface FcmService {
    int sendMessageTo(ClubApplication clubApplication, ClubApplicationStatus clubApplicationResult) throws IOException;

    void refreshFcmToken(FcmTokenRequest fcmTokenRequest);
}
