package cn.ibingli.httpgw.aop;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.jsoup.helper.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import cn.ibingli.common.annotation.aop.CheckUserOnLinePointCut;
import cn.ibingli.common.annotation.aop.ClientUserHeartBeatPointCut;
import cn.ibingli.common.annotation.aop.UserSessionCachePointCut;
import cn.ibingli.common.annotation.aop.UserSessionCheckPointCut;
import cn.ibingli.common.cache.SpringRedisCacheManager;
import cn.ibingli.common.constants.Terminal;
import cn.ibingli.common.constants.cache.CacheConstant;
import cn.ibingli.common.constants.cache.LockKeyConstant;
import cn.ibingli.common.constants.system.UserOnLineStatusEnum;
import cn.ibingli.common.constants.user.AppUserFBStateEnum;
import cn.ibingli.common.constants.user.UserHeartBeatTypeEnum;
import cn.ibingli.common.util.GsonHelper;
import cn.ibingli.common.webtools.UserInfo;
import cn.ibingli.httpgw.aop.vo.AccessTokenInfo;
import cn.ibingli.httpgw.helper.AccessCheckHelper;
import cn.ibingli.httpgw.helper.UserOnLineBizHelper;
import cn.ibingli.httprpc.constant.DefStatusCode.StatusCodeEnum;
import cn.ibingli.httprpc.dto.user.UserDto;
import cn.ibingli.httprpc.exceptions.ApiException;
import cn.ibingli.httprpc.exceptions.SessionExpireException;
import cn.ibingli.module.im.IMAliasHelper;
import cn.ibingli.module.im.IMAliasInfo;
import cn.ibingli.module.im.IMPushTypeEnum;
import cn.ibingli.module.lock.RedisIXReentrantLock;
import cn.ibingli.store.persist.mongo.system.entity.GlobalTopicAlias;
import cn.ibingli.store.persist.mongo.system.service.GlobalTopicAliasService;
import cn.ibingli.store.persist.mongo.user.entity.User;
import cn.ibingli.store.persist.mongo.user.entity.UserOnLine;
import cn.ibingli.store.persist.mongo.user.service.UserOnLineService;
import cn.ibingli.store.persist.mongo.user.service.UserService;
import cn.ibingli.store.persist.mongo.user.vo.ClientAttributeVO;

/**
 * 
 * @description API调用安全验证切片. <br/> 
 *
 * @Date    2017年4月20日 上午10:21:06 <br/> 
 * @author  <a href="mailto:tongyiwzh@qq.com">wuzh</a>
 * @version  
 * @since   JDK1.7
 */
@Aspect
@Order(1)  //设置切面的优先级：如果有多个切面，可通过设置优先级控制切面的执行顺序（数值越小，优先级越高）
@Component
public class SafeAPIAOP {
//	private final static org.apache.logging.log4j.Logger logger = LogManager.getLogger(SafeAPIAOP.class);
	protected static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SafeAPIAOP.class);
	
	@Autowired
	UserService userService;
	
	@Autowired
	UserOnLineService userOnLineService;
	
	@Autowired
	UserOnLineBizHelper userOnLineHelper;
	
	@Autowired
	AccessCheckHelper accessCheckHelper;
	
	@Autowired
	GlobalTopicAliasService globalTopicAliasService;
	
	/**
	 * 
	 * @description 客户端的任何涉及当前用户的api调用，均可用于刷新心跳信息
	 * 
	 * @author    <a mailto:"tongyiwzh@qq.com">wuzh</a>
	 */
	@Pointcut("@annotation(cn.ibingli.common.annotation.aop.ClientUserHeartBeatPointCut)")
	public void clientUserHeartBeatPointCut() {
	}
	
	/**
	 * 
	 * @description 移动端用户登录，强制挤掉其他端的同一用户
	 * 
	 * @author    <a mailto:"tongyiwzh@qq.com">wuzh</a>
	 */
	@Pointcut("@annotation(cn.ibingli.common.annotation.aop.ForceLogoutUserPointCut)")
	public void forceLogoutUserPointCut() {
	}
	
	/**
	 * 
	 * @description 用于用户登录后更新在线状态信息
	 * 
	 * @author    <a mailto:"tongyiwzh@qq.com">wuzh</a>
	 */
	@Pointcut("@annotation(cn.ibingli.common.annotation.aop.UserLoginPointCut)")
	public void userLoginPointCut() {
	}
	
	@Pointcut("@annotation(cn.ibingli.common.annotation.aop.TokenAccessPrivilegePointCut)")
	public void tokenAccessPrivilegePointCut() {
	}
	
	/**
	 * 管理后台用户登录刷新session cache
	 */
	@Pointcut("@annotation(cn.ibingli.common.annotation.aop.UserSessionCachePointCut)")
	public void userSessionCachePointCut() {
	}
	
	/**
	 * 管理后台用户调用api时检查session有效性
	 */
	@Pointcut("@annotation(cn.ibingli.common.annotation.aop.UserSessionCheckPointCut)")
	public void userSessionCheckPointCut() {
	}
	
	/**
	 * 
	 * @description 客户端的任何涉及当前用户的api调用，均可用于刷新心跳信息
	 * 
	 * @author    <a mailto:"tongyiwzh@qq.com">wuzh</a>
	 * @param point
	 * @param returnValue
	 */
	@AfterReturning(pointcut = "clientUserHeartBeatPointCut()", returning = "returnValue")
	public void userOperationHeartBeat(JoinPoint point, Object returnValue) {
		//logger.info("=====================> aop.afterClientUserOperation called, returnValue:" + returnValue);
		UserInfo userInfo = UserInfo.current.get();
		if (userInfo.isIgnorProxy()) {
			logger.info("========> [SafeAPIAOP userOperationHeartBeat] 上层方法忽略了代理逻辑，不再做相关处理...");
			return;
		}
		
		String apiUserId = userInfo.getUserId();
		String deviceId = userInfo.getDeviceId();
		String realAlias = userInfo.getAlias();
		
		try {
//			AccessTokenInfo accessToken = accessCheckHelper.getToken(point);
//			if (accessToken != null) {
//				// 有效的特权访问，不用做后续操作
//				return;
//			}
			// 2019-02-22 wuzh
			if (userInfo.getTerminal() == Terminal.WEB) {
				return;
			}
			
//			if (userOnLineHelper.checkValidManager(apiUserId, userInfo.getCaller(), userInfo.getSid(), userInfo.getNetIp())) {
//				// 管理后台来的api请求，不做处理
//				return;
//			}
			
			if (StringUtils.isBlank(apiUserId) || StringUtils.isBlank(apiUserId)) {
				logger.info("[SafeAPIAOP userOperationHeartBeat] miss parameters, apiUserId:" + apiUserId + ", deviceId:" + deviceId);
				return;
			}
			logger.info("[SafeAPIAOP userOperationHeartBeat] apiUserId:" + apiUserId + ", deviceId:" + deviceId);
			
			MethodSignature signature = (MethodSignature) point.getSignature();
			Method method = signature.getMethod();
			boolean refreshUserOnlineTime = true;
			if (method.getName().equals("heartBeat")) {
				// 不刷新用户活跃时间相关信息
				logger.info("[SafeAPIAOP userOperationHeartBeat] methodName:" + method.getName());
				refreshUserOnlineTime = false;
			}
			
			ClientUserHeartBeatPointCut heartBeatAnn = method.getAnnotation(ClientUserHeartBeatPointCut.class);
			// 用户心跳更新，同时累计用户活跃时长
			userOnLineHelper.refreshHeartbeat(apiUserId, deviceId, userInfo.getTerminal(), heartBeatAnn.heartBeatType());
			
			userOnLineHelper.refreshClientVersion(apiUserId, userInfo.getTerminal(), userInfo.getAppVersion());
			userOnLineHelper.userFBStateSwitch(apiUserId, AppUserFBStateEnum.FRONTSTAGE);
			
			// 可疑的alias值
			boolean forceRefreshAlias = true;
			String alias = realAlias;
			if (StringUtils.isBlank(realAlias)) {
				forceRefreshAlias = false;
			}
						
			// 更新数据库记录
			UserOnLine cnd = new UserOnLine();
			cnd.setUserId(apiUserId);
			cnd.setDeviceId(deviceId);
			
			UserOnLine updateData = new UserOnLine();
			updateData.setStatus(UserOnLineStatusEnum.ACTIVE.getCode());
			if(refreshUserOnlineTime) {
				updateData.setUpdateTime(new Date());
			} else {
				logger.info("[SafeAPIAOP userOperationHeartBeat] 不更新updateTime");
			}
			
			// 将早期缺失数据填充进去
			UserOnLine existOnLineEntity = userOnLineService.cacheQueryOne(cnd);
			if (existOnLineEntity == null) {
				logger.error("[SafeAPIAOP userOperationHeartBeat] fail to get the userOnLine data for[userId:" + apiUserId + ", deviceId:" + deviceId + "]");
				return;
			}
			
			// 为了兼容早期逻辑不严谨导致的问题
			if (StringUtils.isBlank(alias)) {
				alias = existOnLineEntity.getAlias();
			}
			IMAliasInfo imInfo = IMAliasHelper.getClientIMInfo(userInfo.getTerminal(), userInfo.getUserId(), userInfo.getDeviceModel(), userInfo.getAppVersion(), userInfo.getOsVersion());
			// 存在用户登录前对应的alias缓存为空（从设备正常退出），或alias是其他账号的alias值的情况（应该alias缓存还是被清掉）
			String pushAlias = IMAliasHelper.getPushAlias(imInfo.getImpushType(), userInfo.getUserId(), deviceId);
			if (!forceRefreshAlias && StringUtils.isNoneBlank(pushAlias)) {
				alias = pushAlias;
			}
			if (StringUtil.isBlank(alias) || imInfo.getImpushType() == IMPushTypeEnum.YUNBA_PUSH) {
				// 如果强制配置了使用云巴来推送，则即使是可信的alias也不使用
				alias = IMAliasHelper.buildYunbaAlias(apiUserId, deviceId);
			}
			
			logger.info("[SafeAPIAOP userOperationHeartBeat] 用户[" + apiUserId + "]调用api，刷新其alias信息为[" + alias + "], forceRefreshAlias:" + forceRefreshAlias);
			IMAliasHelper.onClientActive(apiUserId, userInfo.getTerminal(), imInfo.getBrand(), imInfo.getImpushType(), alias, forceRefreshAlias);
			
			// userOnline表中存储的是一个更接近推送使用的值？而非app中获取的真实alias值？
			updateData.setAlias(alias);
			if (StringUtils.isNoneBlank(userInfo.getAppVersion())) {
				ClientAttributeVO clientAttr = new ClientAttributeVO();
				updateData.setClientAttrInfo(clientAttr);
				if (StringUtils.isNoneBlank(userInfo.getAppVersion())) {
					clientAttr.setAppVersion(userInfo.getAppVersion());
				}
				if (StringUtils.isNoneBlank(userInfo.getDeviceModel())) {
					clientAttr.setDeviceModel(userInfo.getDeviceModel());
				}
				if (StringUtils.isNoneBlank(userInfo.getOsVersion())) {
					clientAttr.setOsVersion(userInfo.getOsVersion());
				}
				if (StringUtils.isNoneBlank(userInfo.getSv())) {
					clientAttr.setSv(userInfo.getSv());
				}
				if (StringUtils.isNoneBlank(userInfo.getUserAgent())) {
					clientAttr.setUseragent(userInfo.getUserAgent());
				}
			}
			
			userOnLineService.cacheUpdateFirst(cnd, updateData);
		} catch (SessionExpireException e) {
			throw new ApiException(StatusCodeEnum.SESSION_EXPIRE);
		} catch (Exception e) {
			logger.error("[SafeAPIAOP userOperationHeartBeat] exception: ", e);
			throw new ApiException(StatusCodeEnum.UNKNOWN_ERROR);
		}
	}

	/**
	 * 
	 * @description 用户以同样的账号在另外一个移动版客户端上登录时，早期正在使用的客户端将收到一个强制登出的app内部消息（不用系统消息）
	 * 
	 * @author    <a mailto:"tongyiwzh@qq.com">wuzh</a>
	 * @param point
	 * @param returnValue
	 */
	@AfterReturning(pointcut = "forceLogoutUserPointCut()", returning = "returnValue")
	public void loginAndEdgeOffOthers(JoinPoint point, Object returnValue) {
		logger.info("[SafeAPIAOP loginAndEdgeOffOthers] called, returnValue:" + returnValue);
		UserInfo userInfo = UserInfo.current.get();
		if (userInfo.isIgnorProxy()) {
			logger.info("[SafeAPIAOP loginAndEdgeOffOthers] 上层方法忽略了代理逻辑，不再做相关处理...");
			return;
		}
		
		if (returnValue == null) {
			return;
		}
		
		try {
			UserDto response = (UserDto)returnValue;
			
			String apiUserId = response.getUserId();
			String deviceId = userInfo.getDeviceId();
			String realAlias = userInfo.getAlias();// 如果此时有值，则信任它
			userInfo.setUserId(apiUserId);
			
//			if (userOnLineHelper.checkValidManager(apiUserId, userInfo.getCaller(), userInfo.getSid(), userInfo.getNetIp())) {
//				// 管理后台来的api请求，不做处理
//				return;
//			}
			
			// 刷新缓存中的别名信息
			// 最新android系统：如果在登录时能准确拿到有效alias，则在api中会同时传入该值（此时android不会再启动线程获取并上报真实token），如果拿不到，则是空值
			String deviceModelInfo = userInfo.getDeviceModel();
			// 可疑的alias值
			boolean forceRefreshAlias = true;
			String alias = IMAliasHelper.buildYunbaAlias(apiUserId, deviceId);
			if (StringUtils.isNoneBlank(realAlias)) {
				// 如果用户登录时携带了 alias信息，则该值较可信
				alias = realAlias;
			} else {
				// 如果用户登录时未携带 alias信息，则标记将通过其他方式推理一个较可信的 alias
				forceRefreshAlias = false;
			}
			
			userOnLineHelper.refreshHeartbeat(apiUserId, deviceId, userInfo.getTerminal(), UserHeartBeatTypeEnum.USER_LOGIN);
			
			IMAliasInfo imInfo = IMAliasHelper.getClientIMInfo(userInfo.getTerminal(), userInfo.getUserId(), deviceModelInfo, userInfo.getAppVersion(), userInfo.getOsVersion());
			// 2017-12-05 wuzh
//			if (imInfo.getAdapterType() == IMPushTypeEnum.IOS_PUSH) {
//				String globalAlias = IMAliasHelper.getPushAlias(userInfo.getTerminal(), userInfo.getUserId(), deviceId);
//			}
			String globalAlias = IMAliasHelper.getPushAlias(imInfo.getImpushType(), userInfo.getUserId(), deviceId);
			if (!forceRefreshAlias) {
				if (StringUtils.isNoneBlank(globalAlias)) {
					// 如果拿不到可信的alias值，则历史中的alias比猜测组装的alias更可信
					alias = globalAlias;
					logger.info("[SafeAPIAOP loginAndEdgeOffOthers] 未拿到用户:" + userInfo.getUserId() + " 的可信alias，现从缓存中拿一个较可信的值初始化alias:" + alias);
				} else {
					// 尝试从 globalTopicAlias 表拿
					GlobalTopicAlias lastAlisInfo = globalTopicAliasService.getLastAliasInfo(userInfo.getUserId(), userInfo.getTerminal().getCode());
					if (lastAlisInfo != null) {
						alias = lastAlisInfo.getAlias();
						logger.info("[SafeAPIAOP loginAndEdgeOffOthers] 未拿到用户:" + userInfo.getUserId() + " 的可信alias，现从历史记录中拿一个较可信的值初始化alias:" + alias);
					} else {
						logger.warn("[SafeAPIAOP loginAndEdgeOffOthers] 未拿到用户:" + userInfo.getUserId() + " 的可信alias，也未能从历史信息中提取一个较可信的alias，现配置一个默认的alias:" + alias);
					}
				}
			}
			if (imInfo.getImpushType() == IMPushTypeEnum.YUNBA_PUSH) {
				// 如果强制配置了使用云巴来推送，则即使是可信的alias也不使用
				// 上面可能因 forceRefreshAlias 判断，未能进入将 globalAlias 赋值 alias的逻辑
				alias = IMAliasHelper.buildYunbaAlias(apiUserId, deviceId);
			}
			// 更新缓存：persist_user_alias_data
			IMAliasHelper.onClientActive(apiUserId, userInfo.getTerminal(), imInfo.getBrand(), imInfo.getImpushType(), alias, forceRefreshAlias);
			// 更新缓存：persist_global_device_alias_map
			IMAliasHelper.addUserDeviceMapAlias(apiUserId, deviceId, StringUtils.isBlank(globalAlias) ? alias : globalAlias, forceRefreshAlias);
			
			// 动态增加全局alias信息
			// 在 [UserApiServiceImpl registGlobalTopic] 服务中增加缓存比较合适
//			String globalAliasCacheKey = CacheKeyConstant.PERSIST_GLOBAL_TOPIC_ALIAS_DATA;
//			String globalAliasInfo = imInfo.getTerminal().getCode() + ":" + imInfo.getBrand().getCode() + ":" + imInfo.getImpushType().getCode() + ":" + alias;
//			SpringRedisCacheManager.getInstance().sAdd(globalAliasCacheKey, globalAliasInfo);
			
			List<UserOnLine> invalidList = userOnLineService.getMustLogoutClient(apiUserId, deviceId);
			if (CollectionUtils.isEmpty(invalidList)) {
				return;
			}
			
			Date now = new Date();
			String pushAlias = "";
			for (UserOnLine oneRecord : invalidList) {
				logger.info("[SafeAPIAOP loginAndEdgeOffOthers] 退出其他客户端[userId:" + oneRecord.getUserId() + ", deviceId:" + oneRecord.getDeviceId() + "]");
				pushAlias = oneRecord.getAlias();
				// 其他手机上的同一用户登录后，有可能其 registGlobalTopic 操作已经将本机产生的设备-alias映射缓存值给删除了；
				// 此时还是 userOnline表中记录的alias值比较逼近真实推送时使用的alias值
				IMAliasInfo oneImpushInfo = IMAliasHelper.getClientIMInfo(Terminal.codeOf(oneRecord.getTerminalCode()), oneRecord.getUserId(), oneRecord.fetchDeviceModel(), oneRecord.fetchAppVersion(), oneRecord.fetchOsVersion());
				// 从缓存：persist_global_device_alias_map 中提取指定设备上的用户alias
				globalAlias = IMAliasHelper.getPushAlias(oneImpushInfo.getImpushType(), oneRecord.getUserId(), oneRecord.getDeviceId());
				if (StringUtils.isNoneBlank(globalAlias)) {
					pushAlias = globalAlias;
				}
				userOnLineHelper.forceLogoutClient(Terminal.codeOf(oneRecord.getTerminalCode()), oneRecord.getUserId(), oneRecord.getDeviceId(), pushAlias, oneImpushInfo.getImpushType());
			}
		} catch (SessionExpireException e) {
			throw new ApiException(StatusCodeEnum.SESSION_EXPIRE);
		} catch (Exception e) {
			logger.error("[SafeAPIAOP loginAndEdgeOffOthers] exception: ", e);
			throw new ApiException(StatusCodeEnum.UNKNOWN_ERROR);
		}
	}
	
	/**
	 * 
	 * @description 医生用户登录系统后，将刷新用户的最新登录信息
	 * 
	 * @author    <a mailto:"tongyiwzh@qq.com">wuzh</a>
	 * @param point
	 * @param returnValue
	 */
	@AfterReturning(pointcut = "userLoginPointCut()", returning = "returnValue")
	public void userLogin(JoinPoint point, Object returnValue) {
		// api异常时是不会进入本代理方法的
		logger.info("=====================> [SafeAPIAOP userLogin] called, returnValue:" + returnValue);
		UserInfo userInfo = UserInfo.current.get();
		if (userInfo.isIgnorProxy()) {
			logger.info("========> [SafeAPIAOP userLogin] 上层方法忽略了代理逻辑，不再做相关处理...");
			return;
		}
		
		if (returnValue == null) {
			// 登录失败
			logger.error("[SafeAPIAOP userLogin] 用户登录失败，不生成在线记录");
			return;
		}
		
		if (!(returnValue instanceof UserDto)) {
			logger.error("[SafeAPIAOP userLogin] 当前API返回对象类型不合预期，不更新用户在线数据");
			return;
		}
		
		UserDto response = (UserDto) returnValue;
		String apiUserId = response.getUserId();
		userInfo.setUserId(apiUserId);
		
//		try {
//			if (userOnLineHelper.checkValidManager(apiUserId, userInfo.getCaller(), userInfo.getSid(), userInfo.getNetIp())) {
//				// 管理后台来的api请求，不做处理
//				return;
//			}
//		} catch (SessionExpireException e) {
//			throw new ApiException(StatusCodeEnum.SESSION_EXPIRE.getCode(), "错误的调用方式");
//		}
		
		String deviceId = userInfo.getDeviceId();
		String localIp = userInfo.getLocalIp();
		String netIp = userInfo.getNetIp();
		Terminal terminal = userInfo.getTerminal();
		String alias = IMAliasHelper.buildYunbaAlias(apiUserId, deviceId);
		if (StringUtils.isNoneBlank(userInfo.getAlias())) {
			// ios的app重新登录，没传alias过来，此处组装的alias可能是一个无效的值
			alias = userInfo.getAlias();
		}
		
		Date now = new Date();
		try {
			UserOnLine onLineInfo = new UserOnLine();
			onLineInfo.setUserId(apiUserId);
			onLineInfo.setDeviceId(deviceId);
			onLineInfo.setAlias(alias);
			onLineInfo.setLocalIp(localIp);
			onLineInfo.setNetIp(netIp);
			onLineInfo.setTerminalCode(terminal.getCode());
			
			ClientAttributeVO clientAttr = new ClientAttributeVO();
			onLineInfo.setClientAttrInfo(clientAttr);
			if (StringUtils.isNoneBlank(userInfo.getAppVersion())) {
				clientAttr.setAppVersion(userInfo.getAppVersion());
			}
			if (StringUtils.isNoneBlank(userInfo.getDeviceModel())) {
				clientAttr.setDeviceModel(userInfo.getDeviceModel());
			}
			if (StringUtils.isNoneBlank(userInfo.getOsVersion())) {
				clientAttr.setOsVersion(userInfo.getOsVersion());
			}
			if (StringUtils.isNoneBlank(userInfo.getSv())) {
				clientAttr.setSv(userInfo.getSv());
			}
			if (StringUtils.isNoneBlank(userInfo.getUserAgent())) {
				clientAttr.setUseragent(userInfo.getUserAgent());
			}
			
			logger.info("[SafeAPIAOP userLogin] 用户[" + apiUserId + "]登录, onLineInfo:" + GsonHelper.toJson(onLineInfo));
			
			userOnLineService.saveOnLine(onLineInfo);
		} catch (Exception e) {
			logger.error("[SafeAPIAOP userLogin] exception:", e);
		}
	}
	
	/**
	 * 
	 * @description 医生用户的api调用前检查是否为当前在线用户，如果不是，拒绝api继续调用，抛出登出错误码
	 * 
	 * @author    <a mailto:"tongyiwzh@qq.com">wuzh</a>
	 * @param point
	 * @return
	 * @throws Throwable
	 */
	@Around(value="@annotation(cn.ibingli.common.annotation.aop.CheckUserOnLinePointCut)")
	public Object userOnLineChecker(ProceedingJoinPoint point) throws Throwable {
		logger.info("========> [SafeAPIAOP userOnLineChecker] ...");
		UserInfo userInfo = UserInfo.current.get();
		if (userInfo.isIgnorProxy()) {
			logger.info("========> [SafeAPIAOP userOnLineChecker] 上层方法忽略了代理逻辑，不再做相关处理...");
			Object[] args = point.getArgs();  
			Object returnValue = point.proceed(args);
			return returnValue;
		}
		
		String userId = userInfo.getUserId();
		String deviceId = userInfo.getDeviceId();
		Terminal terminal = userInfo.getTerminal(); // 从数据库中提取可能更严谨些
		
		AccessTokenInfo accessToken = accessCheckHelper.getToken(point);
		if (accessToken != null && accessToken.getTokenType() != null) {
			// 有效的特权访问，不用做后续操作
			// 特殊业务强制验证 user-device
			try {
				if (userOnLineHelper.checkAcessToken(accessToken)) {
					logger.info("[SafeAPIAOP userOnLineChecker] 用户[userId:" + userId + ", deviceId:" + deviceId + ", token:" + accessToken + "]为特殊业务请求，通过了相关业务验证不再做用户设备相关安全验证！");
					Object[] args = point.getArgs();  
					Object returnValue = point.proceed(args);
					return returnValue;
				} else {
					throw new ApiException(StatusCodeEnum.INTERNAL_ERROR.getCode(), "用户特殊访问权限校验失败");
				}
			} catch (ApiException e) {
				throw e;
			} catch (Exception e) {
				logger.error("操作异常：", e);
				throw e;
			}
		}
		
//		try {
//			if (userOnLineHelper.checkValidManager(userId, userInfo.getCaller(), userInfo.getSid(), userInfo.getNetIp())) {
//				// 管理后台来的api请求，不做拦截
//				logger.info("[SafeAPIAOP userOnLineChecker] 用户[" + userId + "]为来自管理后台的请求，不做额外校验！");
//				Object[] args = point.getArgs();  
//				Object returnValue = point.proceed(args);
//				return returnValue;
//			}
//		} catch (SessionExpireException e) {
//			throw new ApiException(StatusCodeEnum.SESSION_EXPIRE);
//		} catch (Exception e) {
//			if (e instanceof ApiException) {
//				throw e;
//			}
//			throw new ApiException(StatusCodeEnum.INTERNAL_ERROR.getCode(), "用户权限校验失败");
//		}
		
		StatusCodeEnum responseCode = StatusCodeEnum.SUCCESS;
		String message = "";
		boolean interruptMethod = false;
		if (StringUtils.isBlank(userId) || StringUtils.isBlank(deviceId)) {
			message = "请求未携带必须的参数";
			interruptMethod = true;
			responseCode = StatusCodeEnum.INVALID_SYSTEM_PARAMETER;
		} else {
			User userEntity = userService.cacheQueryById(userId);
			if (userEntity == null) {
				message = "用户不存在";
				interruptMethod = true;
				responseCode = StatusCodeEnum.DATA_NOT_EXIST_USER;
			} else {
				if (userEntity.getCurrentMobileClient() == null || !userEntity.getCurrentMobileClient().equals(deviceId)) {
					// 当前用户被登出
					// 移动端用户不支持同时在线
					logger.error("[SafeAPIAOP userOnLineChecker] 用户:" + userId + " 在数据库中的deviceId:" + userEntity.getCurrentMobileClient() + " 传入的deviceId:" + deviceId);
					message = "当前客户端用户已被登出";
					interruptMethod = true;
					responseCode = StatusCodeEnum.USER_LOGOUT;
				}
			}
		}
		
		// TODO 为方便测试，临时关闭
		// interruptMethod = false;
		if (!interruptMethod) {
			Object[] args = point.getArgs();
			Object returnValue = point.proceed(args);
			return returnValue;
		} else {
			// 修改返回值
			throw new ApiException(responseCode.getCode(), message);
		}
	}
	
	@Before(value = "tokenAccessPrivilegePointCut()")
	public void tokenAccessPrivilege(JoinPoint point) {
		logger.info("[SafeAPIAOP tokenAccessPrivilege] called...");
				
		UserInfo userInfo = UserInfo.current.get();
		if (userInfo.isIgnorProxy()) {
			logger.info("[SafeAPIAOP tokenAccessPrivilege] 上层方法忽略了代理逻辑，不再做相关处理...");
			return;
		}
		
		MethodSignature signature = (MethodSignature) point.getSignature();
		Method method = signature.getMethod();
		CheckUserOnLinePointCut normalAccessAnn = method.getAnnotation(CheckUserOnLinePointCut.class);
		if (normalAccessAnn != null) {
			return;
		}
		
		AccessTokenInfo accessToken = accessCheckHelper.getToken(point);
		if (accessToken != null && accessToken.getTokenType() != null) {
			// 有效的特权访问，不用做后续操作
			try {
				if (userOnLineHelper.checkAcessToken(accessToken)) {
					logger.info("[SafeAPIAOP tokenAccessPrivilege] 当前访问为特权访问，deviceId:" + userInfo.getDeviceId() + ", token:" + accessToken + "]为特殊业务请求，通过了相关业务验证不再做用户设备相关安全验证！");
					return;
				} else {
					throw new ApiException(StatusCodeEnum.INTERNAL_ERROR.getCode(), "非法访问资源");
				}
			} catch (ApiException e) {
				throw e;
			} catch (Exception e) {
				logger.error("操作异常：", e);
				throw e;
			}
		}
	}
	
	/**
	 * 
	 * @description 用户的api调用前检查是否为当前在线用户，如果不是，拒绝api继续调用，抛出登出错误码
	 * 
	 * @author    <a mailto:"tongyiwzh@qq.com">wuzh</a>
	 * @param point
	 * @return
	 * @throws Throwable
	 */
	@Around(value="@annotation(cn.ibingli.common.annotation.aop.UserSessionCheckPointCut)")
	public Object sessionCheck(ProceedingJoinPoint point) throws Throwable {
		logger.info("========> [SafeAPIAOP sessionCheck] ...");
		UserInfo userInfo = UserInfo.current.get();
		if (userInfo.isIgnorProxy()) {
			logger.info("========> [SafeAPIAOP sessionCheck] 上层方法忽略了代理逻辑，不再做相关处理...");
			Object[] args = point.getArgs();  
			Object returnValue = point.proceed(args);
			return returnValue;
		}
		
		// 正式的逻辑，通过sessionid来验证用户有效性，以及白名单逻辑
//		try {
//			if (userOnLineHelper.checkValidManager(userInfo.getUserId(), userInfo.getCaller(), userInfo.getSid(), userInfo.getNetIp())) {
//				// 管理后台来的api请求，不做拦截
//				//logger.info("[SafeAPIAOP sessionCheck] 用户[" + userId + "]为来自管理后台的请求，不做额外校验！");
//				Object[] args = point.getArgs();  
//				Object returnValue = point.proceed(args);
//				return returnValue;
//			}
//		} catch (SessionExpireException e) {
//			throw new ApiException(StatusCodeEnum.SESSION_EXPIRE);
//		} catch (ApiException e) {
//			throw e;
//		} catch (Exception e) {
//			throw new ApiException(StatusCodeEnum.INTERNAL_ERROR.getCode(), "用户权限校验失败");
//		}
		
		StatusCodeEnum responseCode = StatusCodeEnum.SUCCESS;
		String message = "";
		boolean interruptMethod = false;
		
		// 测试的时候使用下面的逻辑，省去postman填写有效sessionid的麻烦
		String userId = userInfo.getUserId();
		String sessionId = userInfo.getSid();
		String caller = userInfo.getCaller();
		logger.info("[SafeAPIAOP sessionCheck] userId:" + userId + ", sessionId:" + sessionId + ", caller:" + caller);
		if (!caller.equals(UserOnLineBizHelper.MANAGEMENT_CALLER)) {
			// 不是管理后台的相关的使用者，不需验证session
			Object[] args = point.getArgs();
			Object returnValue = point.proceed(args);
			return returnValue;
		}
		
		MethodSignature signature = (MethodSignature) point.getSignature();
		Method method = signature.getMethod();
		UserSessionCheckPointCut sessionAnn = method.getAnnotation(UserSessionCheckPointCut.class);
		String lockKey = sessionAnn.cacheLock();
		String cacheKey = sessionAnn.cacheName();
		
		if (StringUtils.isBlank(userId) 
				|| StringUtils.isBlank(sessionId)
				|| StringUtils.isBlank(lockKey)
				|| StringUtils.isBlank(cacheKey)) {
			message = "请求未携带必须的参数";
			interruptMethod = true;
			responseCode = StatusCodeEnum.INVALID_SYSTEM_PARAMETER;
		} else {
			// session验证
			String field = userId + ":" + sessionId;
			long now = System.currentTimeMillis();
			Object sessionCacheValue = SpringRedisCacheManager.getInstance().hGet(cacheKey, field);
			System.out.println("==================> sessionCacheValue:" + sessionCacheValue + ", field:" + field);
			if (sessionCacheValue == null) {
				throw new ApiException(StatusCodeEnum.SESSION_EXPIRE);
			}
			
			long sessionExpireTime = Long.parseLong(sessionCacheValue.toString());
			if (sessionExpireTime < now) {
				System.out.println("==================> 当前时间 now :" + now);
				throw new ApiException(StatusCodeEnum.SESSION_EXPIRE);
			}
		}
		
		// TODO 为方便测试，临时关闭
		// interruptMethod = false;
		if (!interruptMethod) {
			Object[] args = point.getArgs();
			Object returnValue = point.proceed(args);
			return returnValue;
		} else {
			// 修改返回值
			throw new ApiException(responseCode.getCode(), message);
		}
	}
	
	/**
	 * 整理管理后台账号的session时间，如果session已过期，此处不负责生成新的session，仅删除，如果未过期，则延长有效期
	 * 
	 * @param point
	 * @param returnValue
	 */
	@AfterReturning(pointcut = "userSessionCachePointCut()", returning = "returnValue")
	public void refreshUserSessionCache(JoinPoint point, Object returnValue) {
		logger.info("========> [SafeAPIAOP refreshUserSessionCache] ...");
		UserInfo userInfo = UserInfo.current.get();
		if (userInfo.isIgnorProxy()) {
			logger.info("========> [SafeAPIAOP refreshUserSessionCache] 上层方法忽略了代理逻辑，不再做相关处理...");
			return;
		}
		
		MethodSignature signature = (MethodSignature) point.getSignature();
		Method method = signature.getMethod();
		if (method.getName().equals("login") && returnValue == null) {
			// 登录失败
			return;
		}
		
		
		String userId = userInfo.getUserId();
		String sessionId = userInfo.getSid();
		String caller = userInfo.getCaller();
		if (!caller.equals(UserOnLineBizHelper.MANAGEMENT_CALLER)) {
			// 不是管理后台的相关的使用者，不需刷新session
			return;
		}
		
		UserSessionCachePointCut sessionAnn = method.getAnnotation(UserSessionCachePointCut.class);
		String lockKey = sessionAnn.cacheLock();
		String cacheKey = sessionAnn.cacheName();
		
		if (StringUtils.isBlank(userId) 
				|| StringUtils.isBlank(sessionId)
				|| StringUtils.isBlank(lockKey)
				|| StringUtils.isBlank(cacheKey)) {
			logger.error("[SafeAPIAOP refreshUserSessionCache] 上下文信息不完整：[userId:" + userId + ", sessionId:" + sessionId + ", lockKey:" + lockKey + ", cacheKey:" + cacheKey + "]");
			return;
		}
		
		long now = System.currentTimeMillis();
		String field = userId + ":" + sessionId;
		if (RedisIXReentrantLock.getInstance().lock(lockKey, 1000)) {
			try {
				Map<String, Object> allSessionInfoMap = SpringRedisCacheManager.getInstance().hGetAll(cacheKey);
				if (allSessionInfoMap == null || allSessionInfoMap.isEmpty()) {
					logger.error("[SafeAPIAOP refreshUserSessionCache] 缓存中已没有session信息：[userId:" + userId + ", sessionId:" + sessionId + ", lockKey:" + lockKey + ", cacheKey:" + cacheKey + "]");
					return;
				}
				
				Set<Entry<String, Object>> mapEntrySet = allSessionInfoMap.entrySet();
				String oneField = null;
				Object oneExpireData = null;
				List<String> removeFieldList = new ArrayList();
				for (Entry<String, Object> oneEntry : mapEntrySet) {
					oneField = oneEntry.getKey();
					oneExpireData = oneEntry.getValue();
					
					// 访问触发式清理旧的session缓存
					long oldExpireTime = Long.parseLong(oneExpireData.toString());
					if (now > oldExpireTime) {
						removeFieldList.add(oneField);
					}
				}
				for (String oneDelField : removeFieldList) {
					// 旧的session对应的用户再次访问时不是刷新该session，而是重新生成新的session，所以此处可以放心删除不必考虑并发问题
					SpringRedisCacheManager.getInstance().hDel(cacheKey, oneDelField);
				}
				
				// 如果传入的sessionid过期，或者是一个无效session，则不做任何理会
				Object sessionInfo = SpringRedisCacheManager.getInstance().hGet(cacheKey, field);
				if (sessionInfo == null) {
					logger.error("[SafeAPIAOP refreshUserSessionCache] session信息已清除：[userId:" + userId + ", sessionId:" + sessionId + ", lockKey:" + lockKey + ", cacheKey:" + cacheKey + "]");
					return;
				}
				
				long oldExpireTime = Long.parseLong(sessionInfo.toString());
				if (now > oldExpireTime) {
					SpringRedisCacheManager.getInstance().hDel(cacheKey, field);
					logger.error("[SafeAPIAOP refreshUserSessionCache] session已过期：[userId:" + userId + ", sessionId:" + sessionId + ", lockKey:" + lockKey + ", cacheKey:" + cacheKey + "]");
					return;
				}
				
				long newExpireTime = now + CacheConstant.ADMIN_USER_SESSION_TIMEOUT;
				SpringRedisCacheManager.getInstance().hSet(cacheKey, field, newExpireTime);
			} finally {
				RedisIXReentrantLock.getInstance().unlock(lockKey);
			}
		} else {
			logger.error("[SafeAPIAOP refreshUserSessionCache] fail to lock the resource[" + lockKey + "]");
		}
	}
	
	
//	private String getToken(JoinPoint point) {
//		MethodSignature signature = (MethodSignature) point.getSignature();
//		Method method = signature.getMethod();
//		TokenAccessPrivilegePointCut tokenAnn = method.getAnnotation(TokenAccessPrivilegePointCut.class);
//		if (tokenAnn == null) {
//			return null;
//		}
//		
//		Object[] args = point.getArgs();  
//		String[] methodParameterNames = signature.getParameterNames();
//		String tokenParamName = tokenAnn.paramName();
//		String tokenValue = null;
//		for (int i = 0; i < methodParameterNames.length; i++) {
//			String paramName = methodParameterNames[i];
//			if (paramName.equals(tokenParamName)) {
//				if (args[i] != null) {
//					tokenValue = args[i].toString();
//					break;
//				}
//			}
//		}
//		
//		return tokenValue;
//	}
	
	public static void main(String[] args) throws Exception {
		long newExpireTime = System.currentTimeMillis() + CacheConstant.ADMIN_USER_SESSION_TIMEOUT;
		System.out.println("=============> newExpireTime:" + newExpireTime);
		System.out.println("=============> cost:" + (1501666493755L-System.currentTimeMillis()));
		
	}
}
