package com.orvo.emailgenerator.model;

import lombok.Data;

@Data
public class MailboxlayerResponse {

    private boolean format_valid;
    private boolean mx_found;
    private boolean smtp_check;
    private float score;
    private boolean catch_all;
    private boolean disposable;
    private boolean role;

}
