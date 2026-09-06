package com.recruitment.model;

/**
 * Model representing a master technical skill in the taxonomy.
 */
public class Skill {
    private int skillId;
    private String name;
    private String category; // 'Programming', 'Database', 'Frontend', 'Backend', 'DevOps', 'Mobile', 'Soft Skills'

    public Skill() {}

    public Skill(int skillId, String name, String category) {
        this.skillId = skillId;
        this.name = name;
        this.category = category;
    }

    public int getSkillId() {
        return skillId;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
