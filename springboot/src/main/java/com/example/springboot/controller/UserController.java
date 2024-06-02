package com.example.springboot.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.HoneyLogs;
import com.example.springboot.common.LogType;
import com.example.springboot.common.Result;
import com.example.springboot.entity.User;
import com.example.springboot.exception.ServiceException;
import com.example.springboot.service.UserService;
import com.example.springboot.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    UserService userService;

    /**
     * 新增用户信息
     */
    @HoneyLogs(operation = "用户", type = LogType.ADD)
    @PostMapping("/user/add")
    public Result add(@RequestBody User user) {
        try {
            userService.save(user);
        } catch (Exception e) {
            if (e instanceof DuplicateKeyException) {
                return Result.error("插入数据库错误");
            } else {
                return Result.error("系统错误");
            }
        }
        return Result.success();
    }

    /**
     * 修改用户信息
     */
    @HoneyLogs(operation = "用户", type = LogType.UPDATE)
    @PutMapping("/user/update")
    public Result update(@RequestBody User user) {
        userService.updateById(user);
        return Result.success();
    }

    /**
     * 删除用户信息
     */
    @HoneyLogs(operation = "用户", type = LogType.DELETE)
    @DeleteMapping("/user/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        User currentUser = TokenUtils.getCurrentUser();
        if (id.equals(currentUser.getId())) {
            throw new ServiceException("当前用户被不能删除");
        }
        userService.removeById(id);
        return Result.success();
    }


    /**
     * 批量删除用户信息
     */
    @HoneyLogs(operation = "用户", type = LogType.BATCH_DELETE)
    @DeleteMapping("/user/delete/batch")
    public Result batchDelete(@RequestBody List<Integer> ids) {  //  [7, 8]
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser != null && currentUser.getId() != null && ids.contains(currentUser.getId())) {
            throw new ServiceException("当前不能删除");
        }
        userService.removeBatchByIds(ids);
        return Result.success();
    }

    /**
     * 查询全部用户信息
     */
    @GetMapping("/user/selectAll")
    public Result selectAll() {
        List<User> userList = userService.list(new QueryWrapper<User>().orderByDesc("id"));  // select * from user order by id desc
        return Result.success(userList);
    }

    /**
     * 根据ID查询用户信息
     */
    @GetMapping("/user/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        User user = userService.getById(id);
        return Result.success(user);
    }


    /**
     * 多条件模糊查询用户信息
     * pageNum 当前的页码
     * pageSize 每页查询的个数
     */
    @GetMapping("/user/selectByPage")
    public Result selectByPage(@RequestParam Integer pageNum,
                               @RequestParam Integer pageSize,
                               @RequestParam String username,
                               @RequestParam String name) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<User>().orderByDesc("id");  // 默认倒序，让最新的数据在最上面
        queryWrapper.like(StrUtil.isNotBlank(username), "username", username);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        // select * from user where username like '%#{username}%' and name like '%#{name}%'
        Page<User> page = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
        return Result.success(page);
    }

    /**
     * 批量导出数据
     */
    @GetMapping("/user/export")
    public void exportData(@RequestParam(required = false) String username,
                           @RequestParam(required = false) String name,
                           @RequestParam(required = false) String ids,
                           @RequestParam(required = false) String role,
                           HttpServletResponse response) throws IOException {
        System.out.println(role);

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        // 根据 role 和 ids 进行条件筛选
        if (StrUtil.isNotBlank(ids)) {
            List<Integer> idsArr1 = Arrays.stream(ids.split(",")).map(Integer::valueOf).collect(Collectors.toList());
            queryWrapper.in("id", idsArr1);
        } else {
            if ("marketer".equals(role)) {
                queryWrapper.eq("role", "user"); // 只导出 role 为 "user" 的数据
            } else if ("root".equals(role)) {
                // 不做限制，导出所有数据
            }
        }

        // 其他查询条件
        queryWrapper.like(StrUtil.isNotBlank(username), "username", username);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);

        List<User> list = userService.list(queryWrapper);
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.write(list, true);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("用户信息表", "UTF-8") + ".xlsx");
        ServletOutputStream outputStream = response.getOutputStream();
        writer.flush(outputStream, true);
        writer.close();
        outputStream.flush();
        outputStream.close();
    }

    /**
     * 批量导入
     * @param file 传入的excel文件对象
     * @return 导入结果
     */
    @PostMapping("/user/import")
    public Result importData(@RequestParam("file") MultipartFile file, @RequestParam("role") String role) throws IOException {
        ExcelReader reader = ExcelUtil.getReader(file.getInputStream());
        List<User> userList = reader.readAll(User.class);

        // 根据角色过滤并转换数据
        List<User> filteredUserList = userList.stream()
                .filter(user -> {
                    if ("root".equals(role)) {
                        return true; // root 角色可以导入所有数据
                    } else if ("marketer".equals(role)) {
                        return !"管理员".equals(user.getRole()); // marketer 角色只能导入角色不为管理员的数据
                    } else {
                        return false; // 其他角色不允许导入
                    }
                })
                .map(user -> {
                    // 将中文角色转换为英文代码
                    String roleCode = convertRoleToCode(user.getRole());
                    user.setRole(roleCode);
                    return user;
                })
                .collect(Collectors.toList());

        // 写入过滤后的数据到数据库
        try {
            userService.saveBatch(filteredUserList);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("数据批量导入错误");
        }
        return Result.success();
    }

    // 将中文角色转换为英文代码
    private String convertRoleToCode(String roleName) {
        switch (roleName) {
            case "用户":
                return "user";
            case "商家":
                return "marketer";
            case "管理员":
                return "root";
            default:
                return ""; // 或抛出异常，根据实际情况处理
        }
    }

}
