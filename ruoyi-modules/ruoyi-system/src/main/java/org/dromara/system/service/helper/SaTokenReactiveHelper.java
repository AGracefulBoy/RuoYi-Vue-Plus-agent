package org.dromara.system.service.helper;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.model.SaStorage;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Sa-Token 响应式编程上下文辅助类
 * 
 * 用于在 Spring Web 项目中使用 Reactor 响应式编程时，
 * 正确传递和恢复 Sa-Token 上下文
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SaTokenReactiveHelper {

    private static final String SA_TOKEN_CONTEXT_KEY = "SA_TOKEN_CONTEXT";
    private static final String SA_STORAGE_KEY = "SA_STORAGE";

    /**
     * 捕获当前线程的 Sa-Token 上下文信息
     * 
     * @return 包含上下文信息的Map
     */
    public static Map<String, Object> captureContext() {
        Map<String, Object> contextMap = new HashMap<>();
        
        try {
            // 捕获 SaTokenContext
            SaTokenContext tokenContext = SaHolder.getContext();
            if (tokenContext != null) {
                contextMap.put(SA_TOKEN_CONTEXT_KEY, tokenContext);
            }
            
            // 捕获 SaStorage（包含登录信息等）
            // 注意：由于SaStorage没有提供遍历所有key的方法，
            // 这里直接保存整个storage对象的引用
            SaStorage storage = SaHolder.getStorage();
            if (storage != null) {
                contextMap.put(SA_STORAGE_KEY, storage);
            }
        } catch (Exception e) {
            log.warn("捕获Sa-Token上下文时出现异常: {}", e.getMessage());
        }
        
        return contextMap;
    }

    /**
     * 恢复 Sa-Token 上下文信息
     * 
     * @param contextMap 上下文信息Map
     */
    public static void restoreContext(Map<String, Object> contextMap) {
        if (contextMap == null || contextMap.isEmpty()) {
            return;
        }
        
        try {
            // 恢复 SaTokenContext
            SaTokenContext tokenContext = (SaTokenContext) contextMap.get(SA_TOKEN_CONTEXT_KEY);
            if (tokenContext != null) {
                SaManager.setSaTokenContext(tokenContext);
            }
            
            // 注意：SaStorage通常是ThreadLocal的，在响应式编程中
            // 由于线程切换，我们主要依赖SaTokenContext的恢复
            // Storage的恢复在此处可能不是必需的，因为它通常与Context关联
        } catch (Exception e) {
            log.warn("恢复Sa-Token上下文时出现异常: {}", e.getMessage());
        }
    }

    /**
     * 为 Flux 添加 Sa-Token 上下文支持
     * 
     * @param flux 原始的Flux
     * @param <T> 元素类型
     * @return 带有上下文的Flux
     */
    public static <T> Flux<T> withContext(Flux<T> flux) {
        // 在当前线程捕获上下文
        Map<String, Object> contextMap = captureContext();
        
        return flux
            .contextWrite(Context.of(SA_TOKEN_CONTEXT_KEY, contextMap))
            .transformDeferred(withContextRestoration());
    }
    
    /**
     * 包装 Flux 以确保整个响应式链都有 Sa-Token 上下文
     * 这个方法会在每个操作符执行前恢复上下文
     * 
     * @param flux 原始的Flux
     * @param <T> 元素类型
     * @return 带有完整上下文支持的Flux
     */
    public static <T> Flux<T> wrapFlux(Flux<T> flux) {
        // 在当前线程捕获上下文
        Map<String, Object> contextMap = captureContext();
        
        return Flux.defer(() -> {
            // 在订阅时恢复上下文
            restoreContext(contextMap);
            return flux;
        })
        .contextWrite(Context.of(SA_TOKEN_CONTEXT_KEY, contextMap))
        .doOnSubscribe(subscription -> restoreContext(contextMap))
        .doOnNext(item -> restoreContext(contextMap))
        .doOnError(error -> restoreContext(contextMap))
        .doOnComplete(() -> restoreContext(contextMap))
        .doOnCancel(() -> restoreContext(contextMap))
        .doOnTerminate(() -> restoreContext(contextMap));
    }

    /**
     * 为 Mono 添加 Sa-Token 上下文支持
     * 
     * @param mono 原始的Mono
     * @param <T> 元素类型
     * @return 带有上下文的Mono
     */
    public static <T> Mono<T> withContext(Mono<T> mono) {
        // 在当前线程捕获上下文
        Map<String, Object> contextMap = captureContext();
        
        return mono
            .contextWrite(Context.of(SA_TOKEN_CONTEXT_KEY, contextMap))
            .transformDeferred(withContextRestorationMono());
    }

    /**
     * 创建一个可以恢复上下文的转换器（用于Flux）
     * 
     * @param <T> 元素类型
     * @return 转换函数
     */
    private static <T> Function<Flux<T>, Flux<T>> withContextRestoration() {
        return flux -> flux.deferContextual(contextView -> {
            // 从Reactor Context中获取Sa-Token上下文
            @SuppressWarnings("unchecked")
            Map<String, Object> contextMap = contextView.getOrDefault(SA_TOKEN_CONTEXT_KEY, new HashMap<>());
            
            return flux.doOnSubscribe(subscription -> {
                // 在订阅时恢复上下文
                restoreContext(contextMap);
            }).doOnNext(item -> {
                // 确保每个元素处理时都有正确的上下文
                restoreContext(contextMap);
            }).doOnError(error -> {
                // 错误处理时也恢复上下文
                restoreContext(contextMap);
            }).doOnComplete(() -> {
                // 完成时恢复上下文
                restoreContext(contextMap);
            });
        });
    }

    /**
     * 创建一个可以恢复上下文的转换器（用于Mono）
     * 
     * @param <T> 元素类型
     * @return 转换函数
     */
    private static <T> Function<Mono<T>, Mono<T>> withContextRestorationMono() {
        return mono -> mono.deferContextual(contextView -> {
            // 从Reactor Context中获取Sa-Token上下文
            @SuppressWarnings("unchecked")
            Map<String, Object> contextMap = contextView.getOrDefault(SA_TOKEN_CONTEXT_KEY, new HashMap<>());
            
            return mono.doOnSubscribe(subscription -> {
                // 在订阅时恢复上下文
                restoreContext(contextMap);
            }).doOnNext(item -> {
                // 确保处理时有正确的上下文
                restoreContext(contextMap);
            }).doOnError(error -> {
                // 错误处理时也恢复上下文
                restoreContext(contextMap);
            }).doOnSuccess(item -> {
                // 成功时恢复上下文
                restoreContext(contextMap);
            });
        });
    }

    /**
     * 在 Flux.create 或 Flux.push 中使用的辅助方法
     * 在 sink 操作前恢复上下文
     * 
     * @param contextMap 上下文信息
     * @param action 要执行的操作
     */
    public static void runWithContext(Map<String, Object> contextMap, Runnable action) {
        restoreContext(contextMap);
        try {
            action.run();
        } catch (Exception e) {
            log.error("执行操作时发生异常", e);
            throw e;
        }
    }

    /**
     * 在 Flux.create 或 Flux.push 中使用的辅助方法
     * 在 sink 操作前恢复上下文并返回结果
     * 
     * @param contextMap 上下文信息
     * @param supplier 要执行的操作
     * @param <T> 返回类型
     * @return 操作结果
     */
    public static <T> T supplyWithContext(Map<String, Object> contextMap, 
                                          java.util.function.Supplier<T> supplier) {
        restoreContext(contextMap);
        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("执行操作时发生异常", e);
            throw e;
        }
    }
}