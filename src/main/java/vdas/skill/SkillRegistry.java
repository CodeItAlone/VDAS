package vdas.skill;

import vdas.intent.Intent;

import java.util.List;
import java.util.Optional;

/**
 * Simple list-based skill registry.
 *
 * Iterates registered skills and returns the first one that can handle
 * the given intent. No reflection, no DI framework.
 */
public class SkillRegistry {

    private final List<Skill> skills;

    public SkillRegistry(List<Skill> skills) {
        this.skills = List.copyOf(skills); // defensive copy, immutable
    }

    /**
     * Finds the first skill that can handle the given intent.
     *
     * @param intent the intent to match
     * @return the matching skill, or empty if none can handle it
     */
    public Optional<Skill> findSkill(Intent intent) {
        for (Skill skill : skills) {
            if (skill.canHandle(intent)) {
                return Optional.of(skill);
            }
        }
        return Optional.empty();
    }
}
