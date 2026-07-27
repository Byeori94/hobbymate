package com.byeori.hobbymate.clubboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.byeori.hobbymate.clubboard.dao.ClubNoticeDao;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeCreateRequest;
import com.byeori.hobbymate.clubboard.dto.ClubNoticeSearchCondition;
import com.byeori.hobbymate.clubboard.service.ClubNoticeService;
import com.byeori.hobbymate.clubboard.vo.ClubNoticeListItem;

@SpringBootTest
@Transactional
class ClubNoticePersistenceIntegrationTest {

    @Autowired
    private ClubNoticeService clubNoticeService;

    @Autowired
    private ClubNoticeDao clubNoticeDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void leaderCanInsertPinnedNoticeUsingCurrentSchemaAndGeneratedKey() {
        List<Map<String, Object>> writers = jdbcTemplate.queryForList(
                """
                SELECT C.CLUB_ID, CM.MEMBER_ID
                FROM HM_CLUB C
                JOIN HM_CLUB_MEMBER CM
                  ON CM.CLUB_ID = C.CLUB_ID
                 AND CM.MEMBER_STATUS = 'ACTIVE'
                 AND CM.CLUB_ROLE IN ('LEADER', 'MANAGER')
                WHERE C.CLUB_STATUS = 'ACTIVE'
                  AND (
                      SELECT COUNT(*)
                      FROM HM_CLUB_POST P
                      WHERE P.CLUB_ID = C.CLUB_ID
                        AND P.POST_TYPE = 'NOTICE'
                        AND P.NOTICE_YN = 'Y'
                        AND P.POST_STATUS = 'ACTIVE'
                        AND P.DELETED_AT IS NULL
                  ) < 5
                ORDER BY C.CLUB_ID, CM.MEMBER_ID
                LIMIT 1
                """);
        Assumptions.assumeFalse(
                writers.isEmpty(),
                "공지 작성 권한과 고정 여유가 있는 ACTIVE 모임이 없습니다.");

        Long clubId = ((Number) writers.get(0).get("CLUB_ID")).longValue();
        Long memberId = ((Number) writers.get(0).get("MEMBER_ID")).longValue();
        ClubNoticeCreateRequest request = new ClubNoticeCreateRequest();
        request.setTitle("공지 작성 스키마 검증");
        request.setContent("테스트 트랜잭션에서 등록 후 자동 롤백되는 공지 내용입니다.");
        request.setPinnedYn("Y");

        Long postId =
                clubNoticeService.createNotice(String.valueOf(clubId), memberId, request);

        Map<String, Object> stored = jdbcTemplate.queryForMap(
                """
                SELECT CLUB_ID,
                       AUTHOR_MEMBER_ID,
                       POST_TYPE,
                       TITLE,
                       NOTICE_YN,
                       POST_STATUS,
                       VIEW_COUNT,
                       DELETED_AT
                FROM HM_CLUB_POST
                WHERE CLUB_POST_ID = ?
                """,
                postId);
        assertThat(((Number) stored.get("CLUB_ID")).longValue()).isEqualTo(clubId);
        assertThat(((Number) stored.get("AUTHOR_MEMBER_ID")).longValue()).isEqualTo(memberId);
        assertThat(stored.get("POST_TYPE")).isEqualTo("NOTICE");
        assertThat(stored.get("TITLE")).isEqualTo("공지 작성 스키마 검증");
        assertThat(stored.get("NOTICE_YN")).isEqualTo("Y");
        assertThat(stored.get("POST_STATUS")).isEqualTo("ACTIVE");
        assertThat(((Number) stored.get("VIEW_COUNT")).longValue()).isZero();
        assertThat(stored.get("DELETED_AT")).isNull();

        ClubNoticeCreateRequest normalRequest = new ClubNoticeCreateRequest();
        normalRequest.setTitle("공지 작성 스키마 검증 일반");
        normalRequest.setContent("고정 공지 다음에 정렬되는 일반 공지를 검증하는 내용입니다.");
        normalRequest.setPinnedYn("N");
        Long normalPostId = clubNoticeService.createNotice(
                String.valueOf(clubId), memberId, normalRequest);

        List<ClubNoticeListItem> ordered = clubNoticeDao.findNotices(
                new ClubNoticeSearchCondition(
                        clubId,
                        "TITLE",
                        "공지 작성 스키마 검증",
                        "%공지 작성 스키마 검증%",
                        1,
                        20));
        assertThat(ordered)
                .extracting(ClubNoticeListItem::postId)
                .startsWith(postId, normalPostId);
    }
}
