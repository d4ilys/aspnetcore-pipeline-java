import com.bestvike.linq.IEnumerable;
import com.bestvike.linq.Linq;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class main {


    public static void main(String[] args) {
        Run();
    }

    public static void Run() {
        //模仿ASP.NET CORE中间件 实现 责任链设计模式
        PipelineBuilder<HttpContext> builder = new PipelineBuilder<>(context -> {
            System.out.println("终结点.");
        });
        builder.Use(builder, (context, next) -> {
                    System.out.println("第一个中间件开始");
                    next.apply();
                    System.out.println("第一个中间件结束");
                }).Use(builder, (context, next) -> {
                    System.out.println("第二个中间件开始");
                    next.apply();
                    System.out.println("第二个中间件结束");
                })
                .Use(builder, (context, next) -> {
                    System.out.println("第三个中间件开始");
                    next.apply();
                    System.out.println("第三个中间件结束");
                });
        Consumer<HttpContext> requestPipeline = builder.Build();
        HttpContext requestContext = new HttpContext();
        requestContext.setRequest("Request");
        requestContext.setRequest("Response");
        requestPipeline.accept(requestContext);
    }
}


@Data
//定义一个Context
class HttpContext {
    public String Request;
    public String Response;
}

//接口
interface IPipelineBuilder<TContext> {
    IPipelineBuilder<TContext> Use(Function<Consumer<TContext>, Consumer<TContext>> middleware);

    Consumer<TContext> Build();

    public IPipelineBuilder<TContext> Use(IPipelineBuilder<TContext> pip, Action2<TContext, Action> action);
}

/// <summary>
/// 接口的实现
/// </summary>
/// <typeparam name="TContext"></typeparam>
class PipelineBuilder<TContext> implements IPipelineBuilder<TContext> {
    Consumer<TContext> _completeFunc;

    private List<Function<Consumer<TContext>, Consumer<TContext>>> _pipelines =
            new ArrayList<>();

    public PipelineBuilder(Consumer<TContext> completeFunc) {
        _completeFunc = completeFunc;
    }

    public IPipelineBuilder<TContext> Use(Function<Consumer<TContext>, Consumer<TContext>> middleware) {
        //先不执行，存储到一个委托的集合
        _pipelines.add(middleware);
        return this;
    }

    //组装中间件
    public Consumer<TContext> Build() {
        //这个是兜底的
        Consumer<TContext> request = _completeFunc;
        IEnumerable<Function<Consumer<TContext>, Consumer<TContext>>> reverse = Linq.of(_pipelines).reverse();
        for (Function<Consumer<TContext>, Consumer<TContext>> pipeline : reverse) {
            request = pipeline.apply(request);
        }
        return request;
    }

    @Override
    public IPipelineBuilder<TContext> Use(IPipelineBuilder<TContext> pip, Action2<TContext, Action> action) {
        //这里传递委托最重要！
        return pip.Use(next ->
        {
            return context ->
            {
                //最终执行的委托
                action.apply(context, () ->
                {
                    next.accept(context);
                });
            };
        });
    }
}


//以下是自定义的函数式接口
@FunctionalInterface
interface Action {
    void apply();
}

interface Action1<T> {
    void apply(T t);
}

@FunctionalInterface
interface Action2<T1, T2> {
    void apply(T1 t1, T2 t2);
}