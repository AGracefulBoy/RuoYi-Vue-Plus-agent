package org.dromara.system.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.system.service.IReRankService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * ReRank服务实现
 */
@RequiredArgsConstructor
@Service
public class ReRankServiceImpl implements IReRankService {

    @Value("${rerank.url:http://www.hangtushuzhi.cn/search}")
    private String url;

    private final OkHttpClient okHttpClient;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    @Override
    public List<Double> reRank(List<List<String>> sentencePairs) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.set("sentence_pairs", sentencePairs);

            Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestBody.toString(), JSON_MEDIA_TYPE))
                .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new ServiceException("调用reRank失败");
                }

                String responseBody = response.body().string();
                JSONObject jsonResponse = JSONUtil.parseObj(responseBody);
                return jsonResponse.getJSONArray("content").toList(Double.class);
            }
        } catch (IOException e) {
            throw new ServiceException("调用reRank失败: " + e.getMessage());
        }
    }
}