package com.mnsoft.gateway.modules.oauth.task;

import com.mnsoft.gateway.modules.oauth.model.RevokeToken;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Author by hiling, Email admin@mn-soft.com, Date on 12/16/2018.
 */
@Slf4j
public abstract class RevokeThread extends Thread {
    /**
     * 每执行一次清除过期Token后sleep的时间（秒），默认1秒
     */
    int loopWait = 5;

    /**
     * 每次移除过期数据时，保留最近几秒的数据，默认5秒
     */
    int reserveTime = 5;

    /**
     * 每次移除的最多行数，避免单次处理数据过多导致数据库性能压力，默认1000条
     */
    int maxRemoveCount = 1000;

    /**
     * 过期的Access Token队列（先进先出）
     * size()要遍历整个集合，很慢，避免使用，判断空可以用isEmpty()
     */
    ConcurrentLinkedQueue<RevokeToken> revokeTokens;

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000 * loopWait);
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error(e.getMessage());
            }
            log.debug("{}: queue isEmpty():{}", this.getName(), revokeTokens.isEmpty());
            ArrayList<RevokeToken> expiredList = getRevokeToken();
            removeRevokeAndExpiredToken(expiredList);
        }
    }

    protected abstract void removeRevokeAndExpiredToken(ArrayList<RevokeToken> revokeList);

    protected ArrayList<RevokeToken> getRevokeToken() {
        ArrayList<RevokeToken> expiredList = new ArrayList();

        try {
            for (int i = 0; i < this.maxRemoveCount; i++) {
                RevokeToken token = this.revokeTokens.peek(); //查询最早一个
                //没有Token或是最近几秒内的token，则不再执行（由于是队列，先进先出，下一个token在该token后插入，因此肯定也在5秒内）。revoke
                if (token == null) {
                    log.debug("{}: 准备清除：token is null！", this.getName());
                    break;
                }

                if (token.getTime().isAfter(LocalDateTime.now().minusSeconds(this.reserveTime))) {
                    log.debug("{}: 准备清除：过期时间未到！ clientId:{}, UserId:{}，过期时间：{}", this.getName(), token.getClientId(), token.getUserId(), token.getTime().toString());
                    break;
                }

                expiredList.add(token);
                this.revokeTokens.poll();  //查询并移除最早一个，如果没有查到，则返回Null
                log.debug("{}: 即将清除：clientId:{}, UserId:{}，过期时间：{}", this.getName(), token.getClientId(), token.getUserId(), token.getTime().toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
        return expiredList;
    }
}
