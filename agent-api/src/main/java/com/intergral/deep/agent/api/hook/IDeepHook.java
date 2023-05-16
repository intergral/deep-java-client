package com.intergral.deep.agent.api.hook;

import com.intergral.deep.agent.api.IDeep;
import com.intergral.deep.agent.api.reflection.IReflection;

public interface IDeepHook
{

    IDeep deepService();

    IReflection reflectionService();
}
