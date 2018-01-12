module "tribunals-case-api" {
  source   = "git@github.com:contino/moj-module-webapp?ref=0.0.78"
  product  = "${var.product}-api"
  location = "${var.location}"
  env      = "${var.env}"
  asename  = "${data.terraform_remote_state.core_apps_compute.ase_name[0]}"

  app_settings = {
    PORT="${var.tca_server_port}"

    AUTH_PROVIDER_SERVICE_CLIENT_KEY="${var.sscs_idam_key}"
    AUTH_PROVIDER_SERVICE_API_URL="${var.authprovider_service_api_url}"

    IDAM_API_URL="${var.idam_api_url}"
    IDAM_USER_ID="${var.idam_user_id}"
    IDAM_ROLE="${var.idam_role}"

    CCD_CASE_WORKER_ID="${var.ccd_case_worker_id}"
    CCD_SERVICE_API_URL="${var.ccd_service_api_url}"

    EMAIL_FROM="${var.appeal_email_from}"
    EMAIL_TO="${var.appeal_email_to}"
    EMAIL_SUBJECT="${var.appeal_email_subject}"
    EMAIL_MESSAGE="${var.appeal_email_message}"
    EMAIL_SERVER_HOST="${var.appeal_email_host}"
    EMAIL_SERVER_PORT="${var.appeal_email_port}"
    EMAIL_SMTP_TLS_ENABLED="${var.appeal_email_smtp_tls_enabled}"
    EMAIL_SMTP_SSL_TRUST="${var.appeal_email_smtp_ssl_trust}"

    IDAM_S2S_AUTH_TOTP_SECRET="${var.idam_s2s_auth_totp_secret}"
    IDAM_S2S_AUTH_MICROSERVICE="${var.idam_s2s_auth_microservice}"
    IDAM_S2S_AUTH_URL="${var.idam_s2s_auth_url}"

    PDF_API_URL="${var.pdf_api_url}"
  }
}
