package com.byeori.hobbymate.club;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.byeori.hobbymate.club.dao.ClubDao;
import com.byeori.hobbymate.club.vo.ClubDetail;
import com.byeori.hobbymate.club.vo.ClubMemberRelation;

@SpringBootTest
class ClubDetailPersistenceIntegrationTest {

    @Autowired
    private ClubDao clubDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void publicDetailAndLoggedInRelationQueriesMatchCurrentSchema() {
        List<Long> clubIds = jdbcTemplate.queryForList(
                """
                SELECT C.CLUB_ID
                FROM HM_CLUB C
                WHERE C.CLUB_STATUS = 'ACTIVE'
                ORDER BY C.CLUB_ID
                LIMIT 1
                """,
                Long.class);
        Assumptions.assumeFalse(clubIds.isEmpty(), "공개 상세를 확인할 ACTIVE 모임이 없습니다.");

        Long clubId = clubIds.get(0);
        ClubDetail detail = clubDao.findPublicClubDetail(clubId);

        assertThat(detail).isNotNull();
        assertThat(detail.clubId()).isEqualTo(clubId);
        assertThat(detail.memberCount()).isGreaterThanOrEqualTo(1);
        assertThat(detail.leaderMemberId()).isNotNull();

        ClubMemberRelation relation =
                clubDao.findClubMemberRelation(clubId, detail.leaderMemberId());
        assertThat(relation).isNotNull();
        assertThat(relation.memberRole()).isEqualTo("LEADER");
        assertThat(relation.memberStatus()).isEqualTo("ACTIVE");
    }
}
