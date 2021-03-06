package cn.fire.gateway.filter.security;

import cn.fire.common.exception.BaseException;
import cn.fire.common.web.core.response.R;
import cn.fire.gateway.config.IgnoreUriConfig;
import cn.fire.gateway.filter.security.enums.MethodEnum;
import cn.fire.gateway.util.RedisUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static cn.fire.gateway.util.Utils.Security.*;

/**
 * 防重放Gateway过滤器
 * @Author: wangzc
 * @Date: 2020/8/21 11:12
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "gateway.request.security.enable",havingValue = "true")
public class SecurityFilter implements GlobalFilter, Ordered {

    private RedisUtil redisUtil;

    @Autowired
    private IgnoreUriConfig ignoreUrl;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();

        //TODO 加入不需要验证参数签名的逻辑
//        if (request.getPath().contextPath().value().contains())

        String timestamp = request.getHeaders().getFirst(H_TIMESTAMP);
        String nonce = request.getHeaders().getFirst(H_NONCE);
        String sign = request.getHeaders().getFirst(H_SIGN);

        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().set("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE);
        response.getHeaders().set("Cache-Control", "no-cache");

        if (StringUtils.isAnyBlank(timestamp,nonce,sign)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            try {
                DataBuffer dataBuffer = response.bufferFactory().wrap(IOUtils.toByteArray(
                        JSONObject.toJSONString(R.fail(BaseException.BaseErrorEnum.REQUEST_SECURITY_VALID_ERROR.getCode(), BaseException.BaseErrorEnum.REQUEST_SECURITY_VALID_ERROR.getDescription()))
                ));
                return response.writeWith(Mono.just(dataBuffer));
            } catch (IOException e) {
                log.error("valid sign param error: {}", e);
            }
        }

        AbstractMethod protect = MethodEnum.getObject(request.getMethod());
        protect.setHttpRequest(request);
        protect.setNonce(nonce);
        protect.setSign(sign);
        protect.setTimestamp(timestamp);

        Boolean bool = protect.isPassed();

        if (Objects.isNull(bool) || Boolean.FALSE.equals(bool)) {

            response.setStatusCode(HttpStatus.FORBIDDEN);
            byte[] responseContent = JSONObject.toJSONString(
                                R.fail(BaseException.BaseErrorEnum.REQUEST_SECURITY_VALID_ERROR.getCode(), BaseException.BaseErrorEnum.REQUEST_SECURITY_VALID_ERROR.getDescription())
                    ).getBytes(StandardCharsets.UTF_8);

            return response.writeWith(Flux.just(response.bufferFactory().wrap(responseContent)));
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
