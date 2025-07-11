package com.roome.domain.point.entity;

import com.roome.domain.furniture.exception.BookshelfMaxLevelException;
import com.roome.domain.furniture.exception.CdRackMaxLevelException;

public enum PointReason {
	// 포인트 적립
	GUESTBOOK_REWARD,  // 방명록 작성 보상 (+10P, 1일 1회)
	FIRST_COME_EVENT,  // 선착순 이벤트 보상 (랜덤 지급)
	DAILY_ATTENDANCE,  // 출석 체크 보상 (하루 1회 랜덤 지급)
	RANK_1,            // 주간 랭킹 1등 (100P)
	RANK_2,            // 주간 랭킹 2등 (70P)
	RANK_3,            // 주간 랭킹 3등 (50P)

	// 포인트 사용
	THEME_PURCHASE,    // 테마 구매
	BOOK_UNLOCK_LV2,   // 도서 제한 해제 (21~30권, 500P)
	BOOK_UNLOCK_LV3,   // 도서 제한 해제 (31권 이상, 1500P)
	CD_UNLOCK_LV2,     // CD 제한 해제 (21~30개, 500P)
	CD_UNLOCK_LV3,     // CD 제한 해제 (31개 이상, 1500P)

	// 포인트 결제
	POINT_PURCHASE_100,
	POINT_PURCHASE_550,
	POINT_PURCHASE_1200,
	POINT_PURCHASE_4000,

	POINT_REFUND_100,
	POINT_REFUND_550,
	POINT_REFUND_1200,
	POINT_REFUND_4000;

	public static PointReason getBookShelfUpgradeReason(int level) {
		if (level == 1) {
			return BOOK_UNLOCK_LV2;
		}
		if (level == 2) {
			return BOOK_UNLOCK_LV3;
		}
		throw new BookshelfMaxLevelException();
	}

	public static PointReason getCdRackUpgradeReason(int level) {
		if (level == 1) {
			return CD_UNLOCK_LV2;
		}
		if (level == 2) {
			return CD_UNLOCK_LV3;
		}
		throw new CdRackMaxLevelException();
	}

}
