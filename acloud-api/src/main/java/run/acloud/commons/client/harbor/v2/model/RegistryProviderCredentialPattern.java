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


package run.acloud.commons.client.harbor.v2.model;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * The registry credential pattern
 */
@Schema(description = "The registry credential pattern")
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2022-06-22T08:06:19.711Z")
public class RegistryProviderCredentialPattern {
  @SerializedName("access_key_type")
  private String accessKeyType = null;

  @SerializedName("access_key_data")
  private String accessKeyData = null;

  @SerializedName("access_secret_type")
  private String accessSecretType = null;

  @SerializedName("access_secret_data")
  private String accessSecretData = null;

  public RegistryProviderCredentialPattern accessKeyType(String accessKeyType) {
    this.accessKeyType = accessKeyType;
    return this;
  }

   /**
   * The access key type
   * @return accessKeyType
  **/
  @Schema(description = "The access key type")
  public String getAccessKeyType() {
    return accessKeyType;
  }

  public void setAccessKeyType(String accessKeyType) {
    this.accessKeyType = accessKeyType;
  }

  public RegistryProviderCredentialPattern accessKeyData(String accessKeyData) {
    this.accessKeyData = accessKeyData;
    return this;
  }

   /**
   * The access key data
   * @return accessKeyData
  **/
  @Schema(description = "The access key data")
  public String getAccessKeyData() {
    return accessKeyData;
  }

  public void setAccessKeyData(String accessKeyData) {
    this.accessKeyData = accessKeyData;
  }

  public RegistryProviderCredentialPattern accessSecretType(String accessSecretType) {
    this.accessSecretType = accessSecretType;
    return this;
  }

   /**
   * The access secret type
   * @return accessSecretType
  **/
  @Schema(description = "The access secret type")
  public String getAccessSecretType() {
    return accessSecretType;
  }

  public void setAccessSecretType(String accessSecretType) {
    this.accessSecretType = accessSecretType;
  }

  public RegistryProviderCredentialPattern accessSecretData(String accessSecretData) {
    this.accessSecretData = accessSecretData;
    return this;
  }

   /**
   * The access secret data
   * @return accessSecretData
  **/
  @Schema(description = "The access secret data")
  public String getAccessSecretData() {
    return accessSecretData;
  }

  public void setAccessSecretData(String accessSecretData) {
    this.accessSecretData = accessSecretData;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RegistryProviderCredentialPattern registryProviderCredentialPattern = (RegistryProviderCredentialPattern) o;
    return Objects.equals(this.accessKeyType, registryProviderCredentialPattern.accessKeyType) &&
        Objects.equals(this.accessKeyData, registryProviderCredentialPattern.accessKeyData) &&
        Objects.equals(this.accessSecretType, registryProviderCredentialPattern.accessSecretType) &&
        Objects.equals(this.accessSecretData, registryProviderCredentialPattern.accessSecretData);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessKeyType, accessKeyData, accessSecretType, accessSecretData);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RegistryProviderCredentialPattern {\n");
    
    sb.append("    accessKeyType: ").append(toIndentedString(accessKeyType)).append("\n");
    sb.append("    accessKeyData: ").append(toIndentedString(accessKeyData)).append("\n");
    sb.append("    accessSecretType: ").append(toIndentedString(accessSecretType)).append("\n");
    sb.append("    accessSecretData: ").append(toIndentedString(accessSecretData)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

