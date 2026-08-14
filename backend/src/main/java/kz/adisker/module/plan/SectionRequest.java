package kz.adisker.module.plan;

import lombok.Data;

@Data
public class SectionRequest {
    private String domain;
    private String domainNameRu;
    private String domainNameKk;
    private String ownerRole;
    private String content;
    private String objectives;
    private String materials;
    private int sortOrder = 0;
}
