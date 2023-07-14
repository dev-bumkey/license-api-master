package run.acloud.api.resource.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import run.acloud.commons.vo.BaseVO;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: wschoi@acornsoft.io
 * Created on 2017. 10. 12.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Schema(description = "PolicyRule holds information that describes a policy rule, but does not contain information about who the rule applies to or which namespace the rule applies to.")
public class PolicyRuleVO extends BaseVO {

    public static final String SERIALIZED_NAME_API_GROUPS = "apiGroups";
    @SerializedName(SERIALIZED_NAME_API_GROUPS)
    @Schema(
            name = SERIALIZED_NAME_API_GROUPS,
            description =  "APIGroups is the name of the APIGroup that contains the resources.  If multiple API groups are specified, any action requested against one of the enumerated resources in any API group will be allowed."
    )
    private List<String> apiGroups = null;

    public PolicyRuleVO apiGroups(List<String> apiGroups) {

        this.apiGroups = apiGroups;
        return this;
    }

    public PolicyRuleVO addApiGroupsItem(String apiGroupsItem) {
        if (this.apiGroups == null) {
            this.apiGroups = new ArrayList<String>();
        }
        this.apiGroups.add(apiGroupsItem);
        return this;
    }

    public static final String SERIALIZED_NAME_NON_RESOURCE_U_R_LS = "nonResourceURLs";
    @SerializedName(SERIALIZED_NAME_NON_RESOURCE_U_R_LS)
    @Schema(
            name = SERIALIZED_NAME_NON_RESOURCE_U_R_LS,
            description =  "NonResourceURLs is a set of partial urls that a user should have access to.  *s are allowed, but only as the full, final step in the path Since non-resource URLs are not namespaced, this field is only applicable for ClusterRoles referenced from a ClusterRoleBinding. Rules can either apply to API resources (such as 'pods' or 'secrets') or non-resource URL paths (such as '/api'),  but not both."
    )
    private List<String> nonResourceURLs = null;

    public PolicyRuleVO nonResourceURLs(List<String> nonResourceURLs) {

        this.nonResourceURLs = nonResourceURLs;
        return this;
    }

    public PolicyRuleVO addNonResourceURLsItem(String nonResourceURLsItem) {
        if (this.nonResourceURLs == null) {
            this.nonResourceURLs = new ArrayList<String>();
        }
        this.nonResourceURLs.add(nonResourceURLsItem);
        return this;
    }

    public static final String SERIALIZED_NAME_RESOURCE_NAMES = "resourceNames";
    @SerializedName(SERIALIZED_NAME_RESOURCE_NAMES)
    @Schema(
            name = SERIALIZED_NAME_RESOURCE_NAMES,
            description =  "ResourceNames is an optional white list of names that the rule applies to.  An empty set means that everything is allowed."
    )
    private List<String> resourceNames = null;

    public PolicyRuleVO resourceNames(List<String> resourceNames) {

        this.resourceNames = resourceNames;
        return this;
    }

    public PolicyRuleVO addResourceNamesItem(String resourceNamesItem) {
        if (this.resourceNames == null) {
            this.resourceNames = new ArrayList<String>();
        }
        this.resourceNames.add(resourceNamesItem);
        return this;
    }

    public static final String SERIALIZED_NAME_RESOURCES = "resources";
    @SerializedName(SERIALIZED_NAME_RESOURCES)
    @Schema(
            name = SERIALIZED_NAME_RESOURCES,
            description =  "Resources is a list of resources this rule applies to.  ResourceAll represents all resources."
    )
    private List<String> resources = null;

    public PolicyRuleVO resources(List<String> resources) {

        this.resources = resources;
        return this;
    }

    public PolicyRuleVO addResourcesItem(String resourcesItem) {
        if (this.resources == null) {
            this.resources = new ArrayList<String>();
        }
        this.resources.add(resourcesItem);
        return this;
    }

    public static final String SERIALIZED_NAME_VERBS = "verbs";
    @SerializedName(SERIALIZED_NAME_VERBS)
    @Schema(
            name = SERIALIZED_NAME_VERBS,
            description =  "Verbs is a list of Verbs that apply to ALL the ResourceKinds and AttributeRestrictions contained in this rule.  VerbAll represents all kinds."
    )
    private List<String> verbs = new ArrayList<String>();

    public PolicyRuleVO verbs(List<String> verbs) {

        this.verbs = verbs;
        return this;
    }

    public PolicyRuleVO addVerbsItem(String verbsItem) {
        this.verbs.add(verbsItem);
        return this;
    }
}
