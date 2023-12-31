/*
 * Harbor API
 * These APIs provide services for manipulating Harbor project.
 *
 * OpenAPI spec version: 1.10.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package run.acloud.commons.client.harbor.v1.model;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Schema(description = "The harbor native report format")
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2021-04-08T09:10:54.107Z")
public class Report {
  @SerializedName("generated_at")
  private String generatedAt = null;

  @SerializedName("severity")
  private String severity = null;

  @SerializedName("vulnerabilities")
  private List<VulnerabilityItem> vulnerabilities = null;

  @SerializedName("scanner")
  private Scanner scanner = null;

  public Report generatedAt(String generatedAt) {
    this.generatedAt = generatedAt;
    return this;
  }


  @Schema(example = "2006-01-02T15:04:05", description = "Time of generating this report")
  public String getGeneratedAt() {
    return generatedAt;
  }

  public void setGeneratedAt(String generatedAt) {
    this.generatedAt = generatedAt;
  }

  public Report severity(String severity) {
    this.severity = severity;
    return this;
  }


  @Schema(example = "high", description = "A standard scale for measuring the severity of a vulnerability.")
  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public Report vulnerabilities(List<VulnerabilityItem> vulnerabilities) {
    this.vulnerabilities = vulnerabilities;
    return this;
  }

  public Report addVulnerabilitiesItem(VulnerabilityItem vulnerabilitiesItem) {
    if (this.vulnerabilities == null) {
      this.vulnerabilities = new ArrayList<VulnerabilityItem>();
    }
    this.vulnerabilities.add(vulnerabilitiesItem);
    return this;
  }


  @Schema(description = "")
  public List<VulnerabilityItem> getVulnerabilities() {
    return vulnerabilities;
  }

  public void setVulnerabilities(List<VulnerabilityItem> vulnerabilities) {
    this.vulnerabilities = vulnerabilities;
  }

  public Report scanner(Scanner scanner) {
    this.scanner = scanner;
    return this;
  }


  @Schema(description = "")
  public Scanner getScanner() {
    return scanner;
  }

  public void setScanner(Scanner scanner) {
    this.scanner = scanner;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Report report = (Report) o;
    return Objects.equals(this.generatedAt, report.generatedAt) &&
        Objects.equals(this.severity, report.severity) &&
        Objects.equals(this.vulnerabilities, report.vulnerabilities) &&
        Objects.equals(this.scanner, report.scanner);
  }

  @Override
  public int hashCode() {
    return Objects.hash(generatedAt, severity, vulnerabilities, scanner);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Report {\n");
    
    sb.append("    generatedAt: ").append(toIndentedString(generatedAt)).append("\n");
    sb.append("    severity: ").append(toIndentedString(severity)).append("\n");
    sb.append("    vulnerabilities: ").append(toIndentedString(vulnerabilities)).append("\n");
    sb.append("    scanner: ").append(toIndentedString(scanner)).append("\n");
    sb.append("}");
    return sb.toString();
  }


  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

