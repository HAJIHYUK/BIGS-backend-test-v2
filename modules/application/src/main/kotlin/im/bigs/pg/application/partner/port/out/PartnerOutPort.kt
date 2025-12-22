package im.bigs.pg.application.partner.port.out

import im.bigs.pg.domain.partner.Partner

/**
 * Partner 조회용 출력 포트.
 * - 단순 조회 외 확장 필요 시 메서드 추가를 고려합니다.
 */
interface PartnerOutPort {
    fun findById(id: Long): Partner?
}
