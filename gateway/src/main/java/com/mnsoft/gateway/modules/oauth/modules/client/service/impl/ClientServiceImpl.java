package com.mnsoft.gateway.modules.oauth.modules.client.service.impl;

import com.mnsoft.common.exception.BusinessException;
import com.mnsoft.common.utils.UuidUtils;
import com.mnsoft.gateway.modules.oauth.constant.ErrorMessage;
import com.mnsoft.gateway.modules.oauth.modules.client.model.Client;
import com.mnsoft.gateway.modules.oauth.modules.client.mapper.ClientMapper;
import com.mnsoft.gateway.modules.oauth.modules.client.service.ClientService;
import com.mnsoft.gateway.modules.oauth.service.AccessTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author by hiling, Email admin@mn-soft.com, Date on 12/1/2018.
 */
@Service
public class ClientServiceImpl implements ClientService {

    @Autowired
    ClientMapper clientMapper;

    @Autowired
    AccessTokenService accessTokenService;

    @Value("${oauth.scope.client:oauth-client}")
    String allowClient;

    public List<Client> getList(String clientId,String clientName,String clientSecret,Integer status, List<String> scope){
        verifyAuthorization(scope);
        return clientMapper.getList(clientId,clientName,clientSecret,status);
    }

    public int insert(Client client, String loginUserId, List<String> scope) {
        verifyAuthorization(scope);

        if (clientMapper.getList(client.getClientId(), null, null, null).size() > 0) {
            throw new BusinessException(ErrorMessage.CLIENT_ID_EXIST);
        }

        if (clientMapper.getList(null, client.getClientName(), null, null).size() > 0) {
            throw new BusinessException(ErrorMessage.CLIENT_NAME_EXIST);
        }

        client.setCreateUser(loginUserId);
        return clientMapper.insert(client);
    }

    public String refreshSecret(String clientId, String currentSecret, String loginUserId, List<String> scope) {
        verifyAuthorization(scope);
        String newSecret = UuidUtils.getUUID();
        int result = clientMapper.refreshSecret(clientId, currentSecret, newSecret, loginUserId);
        if (result > 0) {
            return newSecret;
        }
        return null;
    }

    public int updateStatus(String clientId, Integer status, String loginUserId, List<String> scope) {
        verifyAuthorization(scope);
        return clientMapper.updateStatus(clientId, status, loginUserId);
    }

    private void verifyAuthorization(List<String> scope){
        if (!scope.contains(allowClient)) {
            throw new BusinessException(ErrorMessage.UNAUTHORIZED_ERROR);
        }
    }
}