package com.aegis.dto;

public class PatientCommandDto {

    private String command; // SPEAK, START_COUNTDOWN, CANCEL, CLEAR
    private String text;
    private Integer seconds;

    public PatientCommandDto() {}

    public PatientCommandDto(String command, String text, Integer seconds) {
        this.command = command;
        this.text = text;
        this.seconds = seconds;
    }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Integer getSeconds() { return seconds; }
    public void setSeconds(Integer seconds) { this.seconds = seconds; }
}
