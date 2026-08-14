package kz.adisker.common;

/**
 * Константы кодов ролей. Используются в @PreAuthorize и в бизнес-логике.
 * Должны совпадать со значениями в колонке role_code таблицы users.
 */
public final class RoleCode {

    public static final String SYSTEM_ADMIN  = "SYSTEM_ADMIN";
    public static final String FOUNDER       = "FOUNDER";
    public static final String DIRECTOR      = "DIRECTOR";
    public static final String METHODIST     = "METHODIST";
    public static final String EDUCATOR      = "EDUCATOR";
    public static final String KAZ_TEACHER   = "KAZ_TEACHER";
    public static final String MUSIC_TEACHER = "MUSIC_TEACHER";
    public static final String PE_INSTRUCTOR = "PE_INSTRUCTOR";
    public static final String NURSE         = "NURSE";
    public static final String JANITOR       = "JANITOR";
    public static final String ACCOUNTANT    = "ACCOUNTANT";
    public static final String PARENT        = "PARENT";

    private RoleCode() {
        // utility class
    }
}
