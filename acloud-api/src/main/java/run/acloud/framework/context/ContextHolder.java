/**
 * Copyright ⓒ 2018 Acornsoft. All rights reserved
 * @project     : cocktail-java
 * @category    : run.acloud.framework.context
 * @class       : ContextHolder.java
 * @author      : Gun Kim (gun@acornsoft.io)
 * @date        : 2018. 8. 28 오후 07:30:38
 * @description :
 */
package run.acloud.framework.context;

import lombok.extern.slf4j.Slf4j;
import run.acloud.commons.constants.CommonConstants;
import run.acloud.commons.util.UUIDGenerator;
import run.acloud.commons.vo.ExecutingContextVO;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ContextHolder {
    private static final boolean IS_TEST_CODE = false;

    private static final ThreadLocal<ContextHolder> CONTEXT = new ThreadLocal<ContextHolder>();

    public static boolean isTest() {
        return IS_TEST_CODE;
    }

    protected Map<String, Object> holder = new HashMap<String, Object>();

    public static ContextHolder get() {
        ContextHolder ctx = CONTEXT.get();
        if ( ctx == null ) {
            ctx = new ContextHolder();
            CONTEXT.set( ctx );
        }
        return ctx;
    }

    public static Long tXID() {
        return tXID( false );
    }

    public static Long tXID( boolean generate ) {
        Long txid = (Long) ContextHolder.get().value(CommonConstants.TXID );
        if ( txid != null ) {
            return txid;
        }
        if ( generate ) {
            final long tXID = UUIDGenerator.tXID();
            ContextHolder.get().put( CommonConstants.TXID, tXID );
            return tXID;
        } else {
            return null;
        }
    }

    public static void removeTXID() {
        ContextHolder.get().remove( CommonConstants.TXID );
    }

    public static ExecutingContextVO exeContext() {
        return exeContext(null);
    }

    public static ExecutingContextVO exeContext(ExecutingContextVO execContext ) {
        if ( execContext != null ) {
            ContextHolder.get().remove(CommonConstants.EXE_CONTEXT);
            ContextHolder.get().put( CommonConstants.EXE_CONTEXT, execContext );
            return execContext;
        }
        else {
            ExecutingContextVO current = (ExecutingContextVO) ContextHolder.get().value(CommonConstants.EXE_CONTEXT);
            if (current != null) {
                return current;
            }
        }

        ContextHolder.get().put( CommonConstants.EXE_CONTEXT, new ExecutingContextVO() );
        return (ExecutingContextVO) ContextHolder.get().value(CommonConstants.EXE_CONTEXT);
    }

    public static void removeExeContext() {
        ContextHolder.get().remove( CommonConstants.EXE_CONTEXT);
    }

    public static Map<String, Object> auditProcessingDatas() {
        return auditProcessingDatas(null);
    }

    public static Map<String, Object> auditProcessingDatas(Map<String, Object> auditProcessingDatas ) {
        if ( auditProcessingDatas != null ) {
            ContextHolder.get().remove(CommonConstants.AUDIT_PROCESSING_DATAS);
            ContextHolder.get().put( CommonConstants.AUDIT_PROCESSING_DATAS, auditProcessingDatas );
            return auditProcessingDatas;
        }
        else {
            Map<String, Object> current = (Map<String, Object>) ContextHolder.get().value(CommonConstants.AUDIT_PROCESSING_DATAS);
            if (current != null) {
                return current;
            }
        }

        ContextHolder.get().put( CommonConstants.AUDIT_PROCESSING_DATAS, new HashMap<>() );
        return (Map<String, Object>) ContextHolder.get().value(CommonConstants.AUDIT_PROCESSING_DATAS);
    }

    public static void removeAuditProcessingDatas() {
        ContextHolder.get().remove( CommonConstants.AUDIT_PROCESSING_DATAS);
    }

    public Object put( String key, Object value ) {
        return this.holder.put( key, value );
    }

    public Object remove( String key ) {
        return this.holder.remove( key );
    }

    public Object value( String key ) {
        return this.holder.get( key );
    }

    public void clear() {
        Map<String, Object> map = CONTEXT.get().holder;
        if ( map != null ) {
            map.clear();
        }
        CONTEXT.remove();
    }

//    /**
//     * @return principal
//     */
//    public static UserPrincipal principal() {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if ( auth != null && auth.getPrincipal() != null ) {
//            Object obj = auth.getPrincipal();
//            if ( obj instanceof UserPrincipal ) {
//                return (UserPrincipal) obj;
//            }
//        }
//        return null;
//    }
//
//    /**
//     * @return userseq It's internal userid.
//     */
//    public static Integer userno() {
//        UserPrincipal principal = ContextHolder.principal();
//        if ( principal != null ) {
//            return principal.getUserNo();
//        }
//        return null;
//    }
}
