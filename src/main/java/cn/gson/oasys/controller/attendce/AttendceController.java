package cn.gson.oasys.controller.attendce;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.ibatis.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.qos.logback.core.joran.action.IADataForComplexProperty;
import ch.qos.logback.core.net.SyslogOutputStream;
import cn.gson.oasys.common.StringtoDate;
import cn.gson.oasys.model.dao.attendcedao.AttendceDao;
import cn.gson.oasys.model.dao.attendcedao.AttendceService;
import cn.gson.oasys.model.dao.system.StatusDao;
import cn.gson.oasys.model.dao.system.TypeDao;
import cn.gson.oasys.model.dao.user.UserDao;
import cn.gson.oasys.model.dao.user.UserService;
import cn.gson.oasys.model.entity.attendce.Attends;
import cn.gson.oasys.model.entity.system.SystemStatusList;
import cn.gson.oasys.model.entity.system.SystemTypeList;
import cn.gson.oasys.model.entity.user.User;

@Controller
@RequestMapping("/")
public class AttendceController {

	Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	AttendceDao attenceDao;
	@Autowired
	AttendceService attendceService;
	@Autowired
	UserDao uDao;
	@Autowired
	UserService userService;
	@Autowired
	TypeDao typeDao;
	@Autowired
	StatusDao statusDao;

	List<Attends> alist;
	List<User> uList;
    Date start,end;
    String month_;
	// ??????????????????
	DefaultConversionService service = new DefaultConversionService();

	// ?????? ???????????????
	@RequestMapping("singin")
	public String Datag(HttpSession session, Model model, HttpServletRequest request) throws InterruptedException, UnknownHostException {
		//????????????ip
		InetAddress ia=null;
		ia=ia.getLocalHost();
		String attendip=ia.getHostAddress();
		// ????????????
		String start = "08:00:00", end = "17:00:00";
		service.addConverter(new StringtoDate());
		// ?????????????????????
		long typeId, statusId = 10;
		Attends attends = null;
		Long userId = Long.parseLong(session.getAttribute("userId") + "");
		User user = uDao.findOne(userId);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String nowdate = sdf.format(date);
		// ?????? ???????????????????????????
		SimpleDateFormat sdf3 = new SimpleDateFormat("EEEE");
		// ????????????
		SimpleDateFormat sdf4 = new SimpleDateFormat("HH:mm");
		// ???????????????
		SimpleDateFormat sdf5 = new SimpleDateFormat("HH:mm:ss");

		// ????????????????????????
		String weekofday = sdf3.format(date);
		// ??????
		String hourmin = sdf4.format(date);

		// ?????????
		String hourminsec = sdf5.format(date);
		//System.out.println("??????" + weekofday + "??????" + hourmin + "?????????" + hourminsec);
		//System.out.println(date);
		Long aid = null;

		// ?????????????????????????????????
		Integer count = attenceDao.countrecord(nowdate, userId);
		if (hourminsec.compareTo(end) > 0) {
			// ???17??????????????????
			System.out.println("----????????????");
			model.addAttribute("error", "1");
		}
		if(hourminsec.compareTo("05:00:00") <0){
			//?????????5?????????????????????
			System.out.println("----????????????");
			model.addAttribute("error", "2");
		}
		else if((hourminsec.compareTo("05:00:00") >0)&&(hourminsec.compareTo(end) <0)){
		// ????????????????????????????????????????????????????????????
		if (count == 0) {
			  if (hourminsec.compareTo(end) < 0) {
				// ????????????????????????????????????????????????????????? ????????????????????????????????????
				// ??????id8
				typeId = 8;
				// ??????????????????????????????
				if (hourminsec.compareTo(start) > 0) {
					// ?????????????????? ??????
					statusId = 11;
				} else if (hourminsec.compareTo(start) < 0) {
					statusId = 10;
				}
				attends = new Attends(typeId, statusId, date, hourmin, weekofday, attendip, user);
				attenceDao.save(attends);
			}
		}
		if (count == 1) {
			// ?????????????????????????????????????????????????????????
			// ??????id9
			typeId = 9;
			// ??????????????????????????????
			if (hourminsec.compareTo(end) > 0) {
				// ??????????????????????????????
				statusId = 10;
			} else if (hourminsec.compareTo(end) < 0) {
				// ??????????????????????????????
				statusId = 12;
			}
			attends = new Attends(typeId, statusId, date, hourmin, weekofday, attendip, user);
			attenceDao.save(attends);
		}
		if (count >= 2) {
			// ??????????????????????????? ??????2???????????????????????????
			// ??????id9
			if (hourminsec.compareTo(end) > 0) { // ????????????????????????????????????????????????
				statusId = 10;
			} else if (hourminsec.compareTo(end) < 0) {
				// ????????????????????????????????????????????????
				statusId = 12;
			}
			aid = attenceDao.findoffworkid(nowdate, userId);
			Attends attends2=attenceDao.findOne(aid);
			attends2.setAttendsIp(attendip);
			attenceDao.save(attends2);
			attendceService.updatetime(date, hourmin, statusId, aid);
			Attends aList = attenceDao.findlastest(nowdate, userId);
		}
		}
		// ?????????????????????????????????
		Attends aList = attenceDao.findlastest(nowdate, userId);
		if (aList != null) {
			String type = typeDao.findname(aList.getTypeId());
			model.addAttribute("type", type);
		}
		model.addAttribute("alist", aList);
		return "systemcontrol/signin";
	}

	// ???????????? ?????????????????????
	@RequestMapping(value="attendcelist",method=RequestMethod.GET)
	public String test(HttpServletRequest request,  Model model,HttpSession session,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "baseKey", required = false) String baseKey,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "time", required = false) String time,
			@RequestParam(value = "icon", required = false) String icon) {
		signsortpaging(request, model, session, page, null, type, status, time, icon);
		return "attendce/attendcelist";
	}

	@RequestMapping(value="attendcelisttable",method=RequestMethod.GET)
	public String testdf(HttpServletRequest request,  Model model,HttpSession session,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "baseKey", required = false) String baseKey,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "time", required = false) String time,
			@RequestParam(value = "icon", required = false) String icon) {
		signsortpaging(request, model, session, page, baseKey, type, status, time, icon);
		return "attendce/attendcelisttable";
	}

	
    // ?????????????????????????????????????????????????????????
	@RequestMapping("attendceatt")
	public String testdasf(HttpServletRequest request, HttpSession session,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "baseKey", required = false) String baseKey,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "time", required = false) String time,
			@RequestParam(value = "icon", required = false) String icon,Model model) {
		allsortpaging(request, session, page, baseKey, type, status, time, icon, model);
		return "attendce/attendceview";
	}

	// ????????????
	@RequestMapping("attendcetable")
	public String table(HttpServletRequest request, HttpSession session,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "baseKey", required = false) String baseKey,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "time", required = false) String time,
			@RequestParam(value = "icon", required = false) String icon,Model model) {
		allsortpaging(request, session, page, baseKey, type, status, time, icon, model);
		return "attendce/attendcetable";
	}

	

	

	// ??????
	@RequestMapping("attdelete")
	public String dsfa(HttpServletRequest request, HttpSession session) {
		long aid = Long.valueOf(request.getParameter("aid"));
		attendceService.delete(aid);
		return "redirect:/attendceatt";
	}

	// ?????????
	@RequestMapping("attendcemonth")
	public String test2(HttpServletRequest request, Model model, HttpSession session,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "baseKey", required = false) String baseKey) {
		monthtablepaging(request, model, session, page, baseKey);
		return "attendce/monthtable";
	}

	@RequestMapping("realmonthtable")
	public String dfshe(HttpServletRequest request, Model model, HttpSession session,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "baseKey", required = false) String baseKey) {
		monthtablepaging(request, model, session, page, baseKey);
		return "attendce/realmonthtable";
	}

	

	// ?????????
	@RequestMapping("attendceweek")
	public String test3(HttpServletRequest request, HttpSession session,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "baseKey", required = false) String baseKey) {
		weektablepaging(request, session, page, baseKey);
		return "attendce/weektable";
	}

	@RequestMapping("realweektable")
	public String dsaf(HttpServletRequest request, HttpSession session,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "baseKey", required = false) String baseKey) {
		weektablepaging(request, session, page, baseKey);
		return "attendce/realweektable";

	}

	

	@RequestMapping("attendceedit")
	public String test4(@Param("aid") String aid, Model model,HttpServletRequest request, HttpSession session) {
		Long userid = Long.valueOf(session.getAttribute("userId") + "");
		if (aid == null) {
			model.addAttribute("write", 0);
		} else if (aid != null) {
			long id = Long.valueOf(aid);
			Attends attends = attenceDao.findOne(id);
			model.addAttribute("write", 1);
			model.addAttribute("attends", attends);
		}
		typestatus(request);
		return "attendce/attendceedit";
	}

	@RequestMapping("attendceedit2")
	public String DSAGen(HttpServletRequest request) {
		long id = Long.valueOf(request.getParameter("id"));
		Attends attends = attenceDao.findOne(id);
		request.setAttribute("attends", attends);
		typestatus(request);
		return "attendce/attendceedit2";
	}

	@RequestMapping(value = "attendcesave", method = RequestMethod.GET)
	public void Datadf() {
	}

	// ????????????
	@RequestMapping(value = "attendcesave", method = RequestMethod.POST)
	public String test4(Model model, HttpSession session, HttpServletRequest request) {
		Long userid = Long.parseLong(session.getAttribute("userId") + "");
		String remark = request.getParameter("remark");
		String statusname=request.getParameter("status");
		SystemStatusList statusList=  statusDao.findByStatusModelAndStatusName("aoa_attends_list", statusname);
		long id = Long.parseLong(request.getParameter("id"));
		Attends attends=attenceDao.findOne(id);
		attends.setAttendsRemark(remark);
		attends.setStatusId(statusList.getStatusId());
		attenceDao.save(attends);
		//attendceService.updatereamrk(remark, id);
		return "redirect:/attendceatt";
	}

	// ??????????????????
	private void typestatus(HttpServletRequest request) {
		List<SystemTypeList> type = (List<SystemTypeList>) typeDao.findByTypeModel("aoa_attends_list");
		List<SystemStatusList> status = (List<SystemStatusList>) statusDao.findByStatusModel("aoa_attends_list");
		request.setAttribute("typelist", type);
		request.setAttribute("statuslist", status);
	}

	
	public void setSomething(String baseKey, Object type, Object status, Object time, Object icon, Model model) {
		if(!StringUtils.isEmpty(icon)){
			model.addAttribute("icon", icon);
			if(!StringUtils.isEmpty(type)){
				model.addAttribute("type", type);
				if("1".equals(type)){
					model.addAttribute("sort", "&type=1&icon="+icon);
				}else{
					model.addAttribute("sort", "&type=0&icon="+icon);
				}
			}
			if(!StringUtils.isEmpty(status)){
				model.addAttribute("status", status);
				if("1".equals(status)){
					model.addAttribute("sort", "&status=1&icon="+icon);
				}else{
					model.addAttribute("sort", "&status=0&icon="+icon);
				}
			}
			if(!StringUtils.isEmpty(time)){
				model.addAttribute("time", time);
				if("1".equals(time)){
					model.addAttribute("sort", "&time=1&icon="+icon);
				}else{
					model.addAttribute("sort", "&time=0&icon="+icon);
				}
			}
		}
		if(!StringUtils.isEmpty(baseKey)){
			model.addAttribute("sort", "&baseKey="+baseKey);
		}
	}
	//??????????????????????????????
	private void signsortpaging(HttpServletRequest request, Model model, HttpSession session, int page, String baseKey,
			String type, String status, String time, String icon) {
		Long userid = Long.valueOf(session.getAttribute("userId") + "");
		setSomething(baseKey, type, status, time, icon, model);
		Page<Attends> page2 = attendceService.singlepage(page, baseKey, userid,type, status, time);
		typestatus(request);
		request.setAttribute("alist", page2.getContent());
		for (Attends attends :page2.getContent()) {
			System.out.println(attends);
		}
		request.setAttribute("page", page2);
		request.setAttribute("url", "attendcelisttable");
	}
	//??????????????????????????????
	private void allsortpaging(HttpServletRequest request, HttpSession session, int page, String baseKey, String type,
			String status, String time, String icon, Model model) {
		setSomething(baseKey, type, status, time, icon, model);
		Long userId = Long.parseLong(session.getAttribute("userId") + "");
		List<Long> ids = new ArrayList<>();
		List<User> users = uDao.findByFatherId(userId);
		for (User user : users) {
			ids.add(user.getUserId());
		}
		if (ids.size() == 0) {
			ids.add(0L);
		}
		User user = uDao.findOne(userId);
		typestatus(request);
		Page<Attends> page2 = attendceService.paging(page, baseKey, ids,type, status, time);
		request.setAttribute("alist", page2.getContent());
		request.setAttribute("page", page2);
		request.setAttribute("url", "attendcetable");
	}
	
	//???????????????
	private void weektablepaging(HttpServletRequest request, HttpSession session, int page, String baseKey) {
		String starttime = request.getParameter("starttime");
		String endtime = request.getParameter("endtime");
		// ????????????
		service.addConverter(new StringtoDate());
		Date startdate = service.convert(starttime, Date.class);
		Date enddate = service.convert(endtime, Date.class);
		
		//??????????????????????????????????????????????????????
		Long userId = Long.parseLong(session.getAttribute("userId") + "");
		List<Long> ids = new ArrayList<>();
		Page<User> userspage =userService.findmyemployuser(page, baseKey, userId);
		for (User user : userspage) {
			ids.add(user.getUserId());
		}
		if (ids.size() == 0) {
			ids.add(0L);
		}
		
		//??????????????????????????????????????????????????? ???????????????????????????????????? ??????????????????????????????????????????????????????????????????
		if(startdate!=null&&enddate!=null)
			{start=startdate;end=enddate;}
		if(startdate==null&&enddate==null)
			startdate=start;enddate=end;
			System.out.println("????????????"+startdate+"??????"+enddate);
		List<Attends> alist = attenceDao.findoneweek(startdate, enddate, ids);
		Set<Attends> attenceset = new HashSet<>();
		for (User user : userspage) {
			for (Attends attence : alist) {
				if (attence.getUser().getUserId() == user.getUserId()) {
					attenceset.add(attence);
				}
			}
			user.setaSet(attenceset);
		}
		String[] weekday = { "?????????", "?????????", "?????????", "?????????", "?????????", "?????????", "?????????" };
		request.setAttribute("ulist", userspage.getContent());
		request.setAttribute("page", userspage);
		request.setAttribute("weekday", weekday);
		request.setAttribute("url", "realweektable");
	}
	//?????????
	private void monthtablepaging(HttpServletRequest request, Model model, HttpSession session, int page,
			String baseKey) {
		Integer offnum,toworknum;
		Long userId = Long.parseLong(session.getAttribute("userId") + "");
		List<Long> ids = new ArrayList<>();
		Page<User> userspage =userService.findmyemployuser(page, baseKey, userId);
		for (User user : userspage) {
			ids.add(user.getUserId());
		}
		if (ids.size() == 0) {
			ids.add(0L);
		}
		String month = request.getParameter("month");
		
		if(month!=null)
			month_=month;
		else
			month=month_;
		
		Map<String, List<Integer>> uMap = new HashMap<>();
		List<Integer> result = null;
		
		for (User user : userspage) {
			result = new ArrayList<>();
			//???????????????????????????
			offnum=attenceDao.countoffwork(month, user.getUserId());
			//???????????????????????????
			toworknum=attenceDao.counttowork(month, user.getUserId());
			for (long statusId = 10; statusId < 13; statusId++) {
				//?????????????????????????????????????????????
				if(statusId==12)
					result.add(attenceDao.countnum(month, statusId, user.getUserId())+toworknum-offnum);
				else
				result.add(attenceDao.countnum(month, statusId, user.getUserId()));
			}
			//??????????????????????????????//??????????????? ??????sql???sum?????????????????????????????????????????????
			System.out.println("????????????"+attenceDao.countothernum(month, 46L, user.getUserId()));
			if(attenceDao.countothernum(month, 46L, user.getUserId())!=null)
			result.add(attenceDao.countothernum(month, 46L, user.getUserId()));
			else
				result.add(0);
			if(attenceDao.countothernum(month, 47L, user.getUserId())!=null)
			result.add(attenceDao.countothernum(month, 47L, user.getUserId()));
			else
				result.add(0);
			//?????????????????????????????? ?????????????????????????????? ????????????=30-8-????????????-??????????????????
			//?????????????????????????????????
			Date date=new Date();
			SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM");
			String date_month=sdf.format(date);
			if(month!=null){
				if(month.compareTo(date_month)>=0)
					result.add(0);
				else
				result.add(30-8-offnum);
			}
			
			uMap.put(user.getUserName(), result);
		}
		model.addAttribute("uMap", uMap);
		model.addAttribute("ulist", userspage.getContent());
		model.addAttribute("page", userspage);
		model.addAttribute("url", "realmonthtable");
	}
}
