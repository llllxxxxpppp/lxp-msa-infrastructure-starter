package com.lcs.subscription.presentation;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lcs.subscription.application.service.SubscriptionService;
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
}
