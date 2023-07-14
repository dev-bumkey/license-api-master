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
 * The registry provider info contains the base info and capability declarations of the registry provider
 */
@Schema(description = "The registry provider info contains the base info and capability declarations of the registry provider")
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2022-06-22T08:06:19.711Z")
public class RegistryProviderInfo {
  @SerializedName("endpoint_pattern")
  private RegistryProviderEndpointPattern endpointPattern = null;

  @SerializedName("credential_pattern")
  private RegistryProviderCredentialPattern credentialPattern = null;

  public RegistryProviderInfo endpointPattern(RegistryProviderEndpointPattern endpointPattern) {
    this.endpointPattern = endpointPattern;
    return this;
  }

   /**
   * The endpoint pattern
   * @return endpointPattern
  **/
  @Schema(description = "The endpoint pattern")
  public RegistryProviderEndpointPattern getEndpointPattern() {
    return endpointPattern;
  }

  public void setEndpointPattern(RegistryProviderEndpointPattern endpointPattern) {
    this.endpointPattern = endpointPattern;
  }

  public RegistryProviderInfo credentialPattern(RegistryProviderCredentialPattern credentialPattern) {
    this.credentialPattern = credentialPattern;
    return this;
  }

   /**
   * The credential pattern
   * @return credentialPattern
  **/
  @Schema(description = "The credential pattern")
  public RegistryProviderCredentialPattern getCredentialPattern() {
    return credentialPattern;
  }

  public void setCredentialPattern(RegistryProviderCredentialPattern credentialPattern) {
    this.credentialPattern = credentialPattern;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RegistryProviderInfo registryProviderInfo = (RegistryProviderInfo) o;
    return Objects.equals(this.endpointPattern, registryProviderInfo.endpointPattern) &&
        Objects.equals(this.credentialPattern, registryProviderInfo.credentialPattern);
  }

  @Override
  public int hashCode() {
    return Objects.hash(endpointPattern, credentialPattern);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RegistryProviderInfo {\n");
    
    sb.append("    endpointPattern: ").append(toIndentedString(endpointPattern)).append("\n");
    sb.append("    credentialPattern: ").append(toIndentedString(credentialPattern)).append("\n");
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

