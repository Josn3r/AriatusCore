package net.ariatus.project.api.profile;

public interface ProfileView {

    String name();

    int level();

    long experience();

    long totalExperience();

    int reputation();

    long playtimeSeconds();

    long kills();

    long deaths();

    long blocksBroken();

    long blocksPlaced();
}