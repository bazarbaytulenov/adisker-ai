package kz.adisker.common;

import java.util.Set;

/**
 * Матрица прав: какие роли может назначать (создавать/менять) пользователь
 * с определённой ролью.
 *
 * Иерархия:
 *  - SYSTEM_ADMIN (супер-админ платформы): заводит организации и их главных лиц
 *    (FOUNDER, DIRECTOR). Не раздаёт рядовые роли и не плодит других супер-админов
 *    через обычный эндпоинт создания пользователей.
 *  - DIRECTOR (локальный администратор организации): управляет своими сотрудниками —
 *    может назначать любые роли внутри организации, КРОМE SYSTEM_ADMIN, FOUNDER и DIRECTOR.
 *  - FOUNDER (учредитель): аналитический доступ, пользователей не создаёт.
 *  - Остальные роли пользователей не создают.
 */
public final class RolePermissions {

    /** Роли, которые может назначать SYSTEM_ADMIN. */
    private static final Set<String> SYSTEM_ADMIN_ASSIGNABLE = Set.of(
            RoleCode.FOUNDER,
            RoleCode.DIRECTOR
    );

    /** Роли, которые может назначать DIRECTOR внутри своей организации. */
    private static final Set<String> DIRECTOR_ASSIGNABLE = Set.of(
            RoleCode.METHODIST,
            RoleCode.EDUCATOR,
            RoleCode.KAZ_TEACHER,
            RoleCode.MUSIC_TEACHER,
            RoleCode.PE_INSTRUCTOR,
            RoleCode.NURSE,
            RoleCode.JANITOR,
            RoleCode.ACCOUNTANT
    );

    /**
     * Может ли пользователь с ролью {@code actorRole} назначить роль {@code targetRole}.
     */
    public static boolean canAssign(String actorRole, String targetRole) {
        if (actorRole == null || targetRole == null) {
            return false;
        }
        return switch (actorRole) {
            case RoleCode.SYSTEM_ADMIN -> SYSTEM_ADMIN_ASSIGNABLE.contains(targetRole);
            case RoleCode.DIRECTOR -> DIRECTOR_ASSIGNABLE.contains(targetRole);
            default -> false;
        };
    }

    /** Набор ролей, доступных для назначения данной ролью (для UI). */
    public static Set<String> assignableRoles(String actorRole) {
        if (actorRole == null) {
            return Set.of();
        }
        return switch (actorRole) {
            case RoleCode.SYSTEM_ADMIN -> SYSTEM_ADMIN_ASSIGNABLE;
            case RoleCode.DIRECTOR -> DIRECTOR_ASSIGNABLE;
            default -> Set.of();
        };
    }

    private RolePermissions() {
        // utility class
    }
}
