package com.lcs.subscription.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lcs.subscription.application.dto.response.SubscriptionResponse;
import com.lcs.subscription.application.service.SubscriptionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SubscriptionInternalController.class)
class SubscriptionInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @Test
    void 회원가입_시_무료_구독권을_생성한다() throws Exception {
        mockMvc.perform(post("/internal/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": 1
                                }
                                """))
                .andExpect(status().isCreated());

        verify(subscriptionService).createFreeSubscriptionIfAbsent(1L);
    }

    @Test
    void memberId가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/internal/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 회원_정지_시_활성_구독권을_정지한다() throws Exception {
        mockMvc.perform(post(
                        "/internal/subscriptions/by-member/{memberId}/suspend",
                        1L))
                .andExpect(status().isNoContent());

        verify(subscriptionService).suspendActiveSubscriptions(1L);
    }

    @Test
    void 회원_탈퇴_시_구독권_탈퇴_처리를_수행한다() throws Exception {
        mockMvc.perform(post(
                        "/internal/subscriptions/by-member/{memberId}/withdraw",
                        1L))
                .andExpect(status().isNoContent());

        verify(subscriptionService).processMemberWithdrawal(1L);
    }

    @Test
    void 회원ID로_구독권_목록을_조회한다() throws Exception {
        SubscriptionResponse response = new SubscriptionResponse(
                1L, 1L, 0L, 1L, null, null, null, null, null, null);
        given(subscriptionService.getSubscriptionsByMemberId(1L)).willReturn(List.of(response));

        mockMvc.perform(get("/internal/subscriptions/by-member/{memberId}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].subscriptionId").value(1));

        verify(subscriptionService).getSubscriptionsByMemberId(1L);
    }

    @Test
    void 구독권이_없는_회원은_빈_리스트를_반환한다() throws Exception {
        given(subscriptionService.getSubscriptionsByMemberId(2L)).willReturn(List.of());

        mockMvc.perform(get("/internal/subscriptions/by-member/{memberId}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(subscriptionService).getSubscriptionsByMemberId(2L);
    }
}
