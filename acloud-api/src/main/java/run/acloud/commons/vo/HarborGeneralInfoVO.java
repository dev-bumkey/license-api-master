/*
 * Harbor API
 * These APIs provide services for manipulating Harbor project.
 *
 * OpenAPI spec version: 2.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package run.acloud.commons.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * GeneralInfo
 */
@Schema(title="Harbor GeneralInfo Model", description="Harbor GeneralInfo Model")
public class HarborGeneralInfoVO {

  @SerializedName("with_notary")
  private Boolean withNotary = null;

  @SerializedName("registry_url")
  private String registryUrl = null;

  @SerializedName("external_url")
  private String externalUrl = null;

  @SerializedName("auth_mode")
  private String authMode = null;

  @SerializedName("project_creation_restriction")
  private String projectCreationRestriction = null;

  @SerializedName("self_registration")
  private Boolean selfRegistration = null;

  @SerializedName("has_ca_root")
  private Boolean hasCaRoot = null;

  @SerializedName("harbor_version")
  private String harborVersion = null;

  public HarborGeneralInfoVO withNotary(Boolean withNotary) {
    this.withNotary = withNotary;
    return this;
  }

   /**
   * If the Harbor instance is deployed with nested notary.
   * @return withNotary
  **/
  @Schema(description = "If the Harbor instance is deployed with nested notary.")
  public Boolean isWithNotary() {
    return withNotary;
  }

  public void setWithNotary(Boolean withNotary) {
    this.withNotary = withNotary;
  }

  public HarborGeneralInfoVO registryUrl(String registryUrl) {
    this.registryUrl = registryUrl;
    return this;
  }

   /**
   * The url of registry against which the docker command should be issued.
   * @return registryUrl
  **/
  @Schema(description = "The url of registry against which the docker command should be issued.")
  public String getRegistryUrl() {
    return registryUrl;
  }

  public void setRegistryUrl(String registryUrl) {
    this.registryUrl = registryUrl;
  }

  public HarborGeneralInfoVO externalUrl(String externalUrl) {
    this.externalUrl = externalUrl;
    return this;
  }

   /**
   * The external URL of Harbor, with protocol.
   * @return externalUrl
  **/
  @Schema(description = "The external URL of Harbor, with protocol.")
  public String getExternalUrl() {
    return externalUrl;
  }

  public void setExternalUrl(String externalUrl) {
    this.externalUrl = externalUrl;
  }

  public HarborGeneralInfoVO authMode(String authMode) {
    this.authMode = authMode;
    return this;
  }

   /**
   * The auth mode of current Harbor instance.
   * @return authMode
  **/
  @Schema(description = "The auth mode of current Harbor instance.")
  public String getAuthMode() {
    return authMode;
  }

  public void setAuthMode(String authMode) {
    this.authMode = authMode;
  }

  public HarborGeneralInfoVO projectCreationRestriction(String projectCreationRestriction) {
    this.projectCreationRestriction = projectCreationRestriction;
    return this;
  }

   /**
   * Indicate who can create projects, it could be &#39;adminonly&#39; or &#39;everyone&#39;.
   * @return projectCreationRestriction
  **/
  @Schema(description = "Indicate who can create projects, it could be 'adminonly' or 'everyone'.")
  public String getProjectCreationRestriction() {
    return projectCreationRestriction;
  }

  public void setProjectCreationRestriction(String projectCreationRestriction) {
    this.projectCreationRestriction = projectCreationRestriction;
  }

  public HarborGeneralInfoVO selfRegistration(Boolean selfRegistration) {
    this.selfRegistration = selfRegistration;
    return this;
  }

   /**
   * Indicate whether the Harbor instance enable user to register himself.
   * @return selfRegistration
  **/
  @Schema(description = "Indicate whether the Harbor instance enable user to register himself.")
  public Boolean isSelfRegistration() {
    return selfRegistration;
  }

  public void setSelfRegistration(Boolean selfRegistration) {
    this.selfRegistration = selfRegistration;
  }

  public HarborGeneralInfoVO hasCaRoot(Boolean hasCaRoot) {
    this.hasCaRoot = hasCaRoot;
    return this;
  }

   /**
   * Indicate whether there is a ca root cert file ready for download in the file system.
   * @return hasCaRoot
  **/
  @Schema(description = "Indicate whether there is a ca root cert file ready for download in the file system.")
  public Boolean isHasCaRoot() {
    return hasCaRoot;
  }

  public void setHasCaRoot(Boolean hasCaRoot) {
    this.hasCaRoot = hasCaRoot;
  }

  public HarborGeneralInfoVO harborVersion(String harborVersion) {
    this.harborVersion = harborVersion;
    return this;
  }

   /**
   * The build version of Harbor.
   * @return harborVersion
  **/
  @Schema(description = "The build version of Harbor.")
  public String getHarborVersion() {
    return harborVersion;
  }

  public void setHarborVersion(String harborVersion) {
    this.harborVersion = harborVersion;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HarborGeneralInfoVO generalInfo = (HarborGeneralInfoVO) o;
    return Objects.equals(this.withNotary, generalInfo.withNotary) &&
        Objects.equals(this.registryUrl, generalInfo.registryUrl) &&
        Objects.equals(this.externalUrl, generalInfo.externalUrl) &&
        Objects.equals(this.authMode, generalInfo.authMode) &&
        Objects.equals(this.projectCreationRestriction, generalInfo.projectCreationRestriction) &&
        Objects.equals(this.selfRegistration, generalInfo.selfRegistration) &&
        Objects.equals(this.hasCaRoot, generalInfo.hasCaRoot) &&
        Objects.equals(this.harborVersion, generalInfo.harborVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(withNotary, registryUrl, externalUrl, authMode, projectCreationRestriction, selfRegistration, hasCaRoot, harborVersion);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GeneralInfo {\n");
    
    sb.append("    withNotary: ").append(toIndentedString(withNotary)).append("\n");
    sb.append("    registryUrl: ").append(toIndentedString(registryUrl)).append("\n");
    sb.append("    externalUrl: ").append(toIndentedString(externalUrl)).append("\n");
    sb.append("    authMode: ").append(toIndentedString(authMode)).append("\n");
    sb.append("    projectCreationRestriction: ").append(toIndentedString(projectCreationRestriction)).append("\n");
    sb.append("    selfRegistration: ").append(toIndentedString(selfRegistration)).append("\n");
    sb.append("    hasCaRoot: ").append(toIndentedString(hasCaRoot)).append("\n");
    sb.append("    harborVersion: ").append(toIndentedString(harborVersion)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

